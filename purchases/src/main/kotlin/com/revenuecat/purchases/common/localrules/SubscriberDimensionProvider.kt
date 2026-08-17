@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.models.Transaction
import kotlinx.coroutines.CancellationException
import java.util.Date

/**
 * Subscriber dimensions: what the customer has bought, and when.
 *
 * Everything but the app user ID is derived from [CustomerInfo], read on every evaluation because a purchase, a
 * renewal or a cancellation lands mid-session and an audience keyed on subscription state has to agree with what
 * the customer's access actually is.
 *
 * Unlike the dashboard, which evaluates the same audience over production purchases only, sandbox purchases count
 * here: a rule has to be testable in a debug build or against a license tester, where every purchase is a sandbox
 * one. [hasMadeSandboxPurchase][KEY_HAS_MADE_SANDBOX_PURCHASE] is the one dimension that reads the flag itself.
 *
 * A dimension the customer has no value for is omitted rather than guessed — an absent key resolves to null in the
 * engine, which is an ordinary non-match. Booleans are the exception: "has never purchased" answers
 * `hasMadeNonSubscriptionPurchase` with a definite `false`, it does not leave it unknown.
 */
internal class SubscriberDimensionProvider(
    private val appUserId: () -> String?,
    private val customerInfo: suspend () -> CustomerInfo,
) : RulesDimensionProvider {

    override val identifier: String = "subscriber"

    override val namespace: RulesDimensionNamespace = RulesDimensionNamespace.Subscriber

    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
        appUserIdDimension() + customerInfoDimensions(date)

    /**
     * Read separately from the rest so a rule targeting the app user ID does not depend on the network: the ID is
     * known as soon as the SDK is configured, while [CustomerInfo] may still be in flight.
     */
    private fun appUserIdDimension(): Map<String, RulesDimensionValue> {
        val appUserId = try {
            appUserId()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The app user ID is unavailable, so the customerId dimension can't be evaluated: $e" }
            null
        }
        return appUserId
            ?.takeIf { it.isNotEmpty() }
            ?.let { mapOf(KEY_CUSTOMER_ID to RulesDimensionValue.StringValue(it)) }
            .orEmpty()
    }

    /**
     * A customer info that cannot be read contributes no dimensions instead of failing the snapshot: the read
     * reaches out through the configured instance, which an app can tear down mid-evaluation, and it can fall back
     * to the network, which can fail. Neither should abort the snapshot and take an otherwise resolvable
     * checkpoint down with it. Cancellation is not a failure and propagates.
     */
    private suspend fun customerInfoDimensions(date: Date): Map<String, RulesDimensionValue> {
        val customerInfo = try {
            customerInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The customer info is unavailable, so subscriber dimensions can't be evaluated: $e" }
            return emptyMap()
        }
        return customerInfo.dimensions(date)
    }

    private fun CustomerInfo.dimensions(date: Date): Map<String, RulesDimensionValue> {
        val subscriptions = subscriptionsByProductIdentifier.values
        val transactions = nonSubscriptionTransactions
        val latestSubscription = subscriptions.maxByOrNull { it.purchaseDate }
        val latestPurchase = latestPurchase(latestSubscription, transactions)
        val trialSubscription = subscriptions
            .filter { it.periodType == PeriodType.TRIAL }
            .maxByOrNull { it.purchaseDate }
        val optOutAt = latestSubscription?.unsubscribeDetectedAt
        val optedOutOfTrial = latestSubscription?.periodType == PeriodType.TRIAL

        return buildMap {
            putString(KEY_ORIGINAL_APP_USER_ID, originalAppUserId)
            putBool(KEY_IS_CURRENTLY_TRIALING, subscriptions.any { it.isActive && it.periodType == PeriodType.TRIAL })
            putBool(KEY_IS_RC_PROMO, latestPurchase?.store == Store.PROMOTIONAL)
            putBool(KEY_HAS_MADE_NON_SUBSCRIPTION_PURCHASE, transactions.isNotEmpty())
            putBool(
                KEY_HAS_MADE_SANDBOX_PURCHASE,
                subscriptions.any { it.isSandbox } || transactions.any { it.isSandbox },
            )
            // Only a subscription has a renewal intent; with none there is nothing to report rather than a `false`.
            latestSubscription?.let { putBool(KEY_LATEST_AUTO_RENEW_INTENT, it.willRenew) }

            putStringList(KEY_ALL_PURCHASED_PRODUCT_IDS, allPurchasedProductIds.sorted())
            putStringList(
                KEY_ANY_ACTIVE_STORE,
                subscriptions.filter { it.isActiveOrInGracePeriod(date) }
                    .map { it.store.stringValue }
                    .distinct()
                    .sorted(),
            )
            putStringList(KEY_LATEST_ENTITLEMENTS, latestEntitlementIdentifiers(latestPurchase))

            putString(KEY_LATEST_STORE, latestPurchase?.store?.stringValue)
            putString(KEY_LATEST_OWNERSHIP_TYPE, latestPurchase?.subscription?.ownershipType?.dimensionValue)

            putDate(KEY_FIRST_SEEN_AT, earliestKnownDate(subscriptions, transactions))
            // The date the backend last answered us, which is when it last saw this customer through this device.
            putDate(KEY_LAST_SEEN_AT, requestDate)
            putDate(KEY_LATEST_EXPIRATION_AT, latestExpirationDate)
            putDate(KEY_MOST_RECENT_PURCHASE_AT, latestPurchase?.purchaseDate)
            // A renewal starts a new subscription period, so it is that period's purchase date.
            putDate(KEY_MOST_RECENT_RENEWAL_AT, latestSubscription?.purchaseDate)
            // Reading only the latest subscription is what makes these go back to absent on a resubscribe.
            putDate(KEY_SUBSCRIPTION_OPT_OUT_AT, optOutAt?.takeUnless { optedOutOfTrial })
            putDate(KEY_TRIAL_OPT_OUT_AT, optOutAt?.takeIf { optedOutOfTrial })
            // Known only while the trial is the current period: once it converts, the SDK no longer carries the
            // trial window.
            putDate(KEY_TRIAL_START_AT, trialSubscription?.purchaseDate)
            putDate(KEY_TRIAL_END_AT, trialSubscription?.expiresDate)
        }
    }

    private fun latestPurchase(
        latestSubscription: SubscriptionInfo?,
        transactions: List<Transaction>,
    ): LatestPurchase? = listOfNotNull(
        latestSubscription?.let {
            LatestPurchase(it.purchaseDate, it.productIdentifier, it.productPlanIdentifier, it.store, subscription = it)
        },
        transactions.maxByOrNull { it.purchaseDate }?.let {
            LatestPurchase(it.purchaseDate, it.productIdentifier, productPlanIdentifier = null, it.store, null)
        },
    ).maxByOrNull { it.purchaseDate }

    private fun CustomerInfo.latestEntitlementIdentifiers(latestPurchase: LatestPurchase?): List<String> =
        latestPurchase?.let { purchase ->
            entitlements.all.values
                .filter { it.unlockedBy(purchase) }
                .map { it.identifier }
                .sorted()
        }.orEmpty()

    /**
     * The dashboard builds this out of the customer's creation date too, which is the backend's alone. The rest of
     * it is here: when this install first saw the customer, and when they first bought anything.
     */
    private fun CustomerInfo.earliestKnownDate(
        subscriptions: Collection<SubscriptionInfo>,
        transactions: List<Transaction>,
    ): Date = (
        listOf(firstSeen) +
            listOfNotNull(originalPurchaseDate) +
            subscriptions.flatMap { listOfNotNull(it.originalPurchaseDate, it.purchaseDate) } +
            transactions.flatMap { listOfNotNull(it.originalPurchaseDate, it.purchaseDate) }
        ).min()

    /**
     * The customer's most recent purchase of any kind. A subscription carries facts a one-time purchase does not,
     * so it rides along for the dimensions only it can answer.
     */
    private class LatestPurchase(
        val purchaseDate: Date,
        val productIdentifier: String,
        val productPlanIdentifier: String?,
        val store: Store,
        val subscription: SubscriptionInfo?,
    )

    internal companion object {
        const val KEY_ALL_PURCHASED_PRODUCT_IDS = "allPurchasedProductIds"
        const val KEY_ANY_ACTIVE_STORE = "anyActiveStore"
        const val KEY_CUSTOMER_ID = "customerId"
        const val KEY_FIRST_SEEN_AT = "firstSeenAt"
        const val KEY_HAS_MADE_NON_SUBSCRIPTION_PURCHASE = "hasMadeNonSubscriptionPurchase"
        const val KEY_HAS_MADE_SANDBOX_PURCHASE = "hasMadeSandboxPurchase"
        const val KEY_IS_CURRENTLY_TRIALING = "isCurrentlyTrialing"
        const val KEY_IS_RC_PROMO = "isRcPromo"
        const val KEY_LAST_SEEN_AT = "lastSeenAt"
        const val KEY_LATEST_AUTO_RENEW_INTENT = "latestAutoRenewIntent"
        const val KEY_LATEST_ENTITLEMENTS = "latestEntitlements"
        const val KEY_LATEST_EXPIRATION_AT = "latestExpirationAt"
        const val KEY_LATEST_OWNERSHIP_TYPE = "latestOwnershipType"
        const val KEY_LATEST_STORE = "latestStore"
        const val KEY_MOST_RECENT_PURCHASE_AT = "mostRecentPurchaseAt"
        const val KEY_MOST_RECENT_RENEWAL_AT = "mostRecentRenewalAt"
        const val KEY_ORIGINAL_APP_USER_ID = "originalAppUserId"
        const val KEY_SUBSCRIPTION_OPT_OUT_AT = "subscriptionOptOutAt"
        const val KEY_TRIAL_END_AT = "trialEndAt"
        const val KEY_TRIAL_OPT_OUT_AT = "trialOptOutAt"
        const val KEY_TRIAL_START_AT = "trialStartAt"

        /**
         * The store keeps serving a subscription while a billing issue is being retried, so a customer in a grace
         * period is one the dashboard counts as buying from that store.
         */
        private fun SubscriptionInfo.isActiveOrInGracePeriod(date: Date): Boolean =
            isActive || gracePeriodExpiresDate?.after(date) == true

        /**
         * The base plan is part of what was bought on Google, so it is part of the comparison: two base plans of
         * one subscription are two different products.
         */
        private fun EntitlementInfo.unlockedBy(purchase: LatestPurchase): Boolean =
            productIdentifier == purchase.productIdentifier &&
                productPlanIdentifier == purchase.productPlanIdentifier

        private val OwnershipType.dimensionValue: String?
            get() = when (this) {
                OwnershipType.PURCHASED -> "PURCHASED"
                OwnershipType.FAMILY_SHARED -> "FAMILY_SHARED"
                // Not a value to compare against: an unknown ownership type is the absence of one.
                OwnershipType.UNKNOWN -> null
            }

        private fun MutableMap<String, RulesDimensionValue>.putBool(key: String, value: Boolean) {
            put(key, RulesDimensionValue.BoolValue(value))
        }

        private fun MutableMap<String, RulesDimensionValue>.putString(key: String, value: String?) {
            if (!value.isNullOrEmpty()) put(key, RulesDimensionValue.StringValue(value))
        }

        private fun MutableMap<String, RulesDimensionValue>.putDate(key: String, value: Date?) {
            if (value != null) put(key, RulesDimensionValue.DateValue(value))
        }

        private fun MutableMap<String, RulesDimensionValue>.putStringList(key: String, values: List<String>) {
            if (values.isNotEmpty()) put(key, RulesDimensionValue.StringListValue(values))
        }
    }
}
