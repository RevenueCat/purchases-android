@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.common.Constants
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.Transaction
import kotlinx.coroutines.CancellationException
import java.util.Date

internal class CustomerInfoDimensionProvider(
    private val customerInfo: suspend (appUserId: String) -> CustomerInfo,
) : RulesDimensionProvider {

    override val namespace: RulesDimensionNamespace = RulesDimensionNamespace.CustomerInfo

    /**
     * The app user ID is reported without waiting for the customer info, so a rule targeting only the ID does not
     * depend on the network: it is known as soon as the SDK is configured.
     */
    override suspend fun dimensions(context: RulesDimensionContext): Map<String, RulesDimensionValue> =
        customerInfoDimensions(context) + buildMap { putString(KEY_APP_USER_ID, context.appUserId) }

    /**
     * A customer info that cannot be read contributes no dimensions rather than failing the snapshot, which would
     * take an otherwise resolvable checkpoint down with it. Cancellation is not a failure and propagates.
     *
     * The records are built inside the same guard, because a [CustomerInfo] parses its purchases out of its raw
     * payload on first access rather than at construction, so a malformed payload surfaces here.
     */
    private suspend fun customerInfoDimensions(context: RulesDimensionContext): Map<String, RulesDimensionValue> {
        // Nobody to ask about: the SDK has no cached user, so there is no request to make on their behalf.
        if (context.appUserId.isEmpty()) return emptyMap()
        return try {
            customerInfo(context.appUserId).dimensions(context.date)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The customer info is unavailable, so customer dimensions can't be evaluated: $e" }
            emptyMap()
        }
    }

    private fun CustomerInfo.dimensions(date: Date): Map<String, RulesDimensionValue> = buildMap {
        putString(KEY_ORIGINAL_APP_USER_ID, originalAppUserId)
        putDate(KEY_FIRST_SEEN_AT, firstSeen)
        // When the backend last saw this customer on any device, which is what "how long since they last opened
        // the app" needs. Deliberately not `requestDate`: that is when the backend last answered *us*, and the
        // SDK refreshes on every foreground, so it would read as "just now" for the whole session.
        putDate(KEY_LAST_SEEN_AT, lastSeen)
        putDate(KEY_ORIGINAL_PURCHASED_AT, originalPurchaseDate)
        putObjectList(KEY_PURCHASES, purchaseRecords(date))
        putObjectList(KEY_ENTITLEMENTS, entitlementRecords())
    }

    /**
     * Newest first, so `purchases.0` is the customer's most recent purchase of any kind and a rule about it needs
     * no iteration at all. Subscriptions are ordered by product before the merge, so purchases that share a date
     * still come out in a defined order.
     */
    private fun CustomerInfo.purchaseRecords(date: Date): List<Map<String, RulesDimensionValue>> {
        val subscriptions = subscriptionsByProductIdentifier.values
            .sortedBy { subscription -> subscription.productIdentifier }
            .map { subscription -> subscription.record(date) }
        // Already sorted by purchase date by `CustomerInfo`.
        val transactions = nonSubscriptionTransactions.map { transaction -> transaction.record() }
        return (subscriptions + transactions).sortedByDescending { record -> record.dateOrNull(KEY_PURCHASED_AT) }
    }

    private fun CustomerInfo.entitlementRecords(): List<Map<String, RulesDimensionValue>> =
        entitlements.all.values
            .sortedBy { entitlement -> entitlement.identifier }
            .map { entitlement -> entitlement.record() }

    private fun SubscriptionInfo.record(date: Date): Map<String, RulesDimensionValue> = buildMap {
        putString(KEY_KIND, KIND_SUBSCRIPTION)
        putString(KEY_PRODUCT_IDENTIFIER, productIdentifier)
        putString(KEY_PRODUCT_PLAN_IDENTIFIER, productPlanIdentifier)
        putString(
            KEY_PURCHASED_PRODUCT_IDENTIFIER,
            purchasedProductIdentifier(productIdentifier, productPlanIdentifier),
        )
        putString(KEY_STORE_TRANSACTION_ID, storeTransactionId)
        putString(KEY_DISPLAY_NAME, displayName)
        putString(KEY_STORE, store.stringValue)
        putString(KEY_OWNERSHIP_TYPE, ownershipType.dimensionValue)
        putString(KEY_PERIOD_TYPE, periodType.dimensionValue)
        putString(KEY_STATUS, status(date))
        putPrice(price)
        putDate(KEY_PURCHASED_AT, purchaseDate)
        putDate(KEY_ORIGINAL_PURCHASED_AT, originalPurchaseDate)
        putDate(KEY_EXPIRES_AT, expiresDate)
        putDate(KEY_UNSUBSCRIBE_DETECTED_AT, unsubscribeDetectedAt)
        putDate(KEY_BILLING_ISSUE_DETECTED_AT, billingIssuesDetectedAt)
        putDate(KEY_GRACE_PERIOD_EXPIRES_AT, gracePeriodExpiresDate)
        putDate(KEY_REFUNDED_AT, refundedAt)
        putDate(KEY_AUTO_RESUME_AT, autoResumeDate)
        putBool(KEY_IS_SANDBOX, isSandbox)
        putBool(KEY_IS_ACTIVE, isActive)
        putBool(KEY_WILL_RENEW, willRenew)
        // The store keeps serving a subscription while a billing issue is being retried, and `isActive` does not
        // cover that, so `{"or": [isActive, isInGracePeriod]}` is the still-being-served test.
        putBool(KEY_IS_IN_GRACE_PERIOD, isInGracePeriod(date))
        putBool(KEY_IS_REFUNDED, refundedAt != null)
        // A resume date is only ever set while a Google subscription is paused, so having one *is* being paused.
        putBool(KEY_IS_PAUSED, autoResumeDate != null)
    }

    private fun Transaction.record(): Map<String, RulesDimensionValue> = buildMap {
        putString(KEY_KIND, KIND_NON_SUBSCRIPTION)
        putString(KEY_PRODUCT_IDENTIFIER, productIdentifier)
        // A one-time purchase has no base plan, so the two forms of the identifier are the same one.
        putString(KEY_PURCHASED_PRODUCT_IDENTIFIER, productIdentifier)
        putString(KEY_TRANSACTION_IDENTIFIER, transactionIdentifier)
        putString(KEY_STORE_TRANSACTION_ID, storeTransactionId)
        putString(KEY_DISPLAY_NAME, displayName)
        putString(KEY_STORE, store.stringValue)
        putPrice(price)
        putDate(KEY_PURCHASED_AT, purchaseDate)
        putDate(KEY_ORIGINAL_PURCHASED_AT, originalPurchaseDate)
        putBool(KEY_IS_SANDBOX, isSandbox)
    }

    private fun EntitlementInfo.record(): Map<String, RulesDimensionValue> = buildMap {
        putString(KEY_IDENTIFIER, identifier)
        putString(KEY_PRODUCT_IDENTIFIER, productIdentifier)
        putString(KEY_PRODUCT_PLAN_IDENTIFIER, productPlanIdentifier)
        putString(
            KEY_PURCHASED_PRODUCT_IDENTIFIER,
            purchasedProductIdentifier(productIdentifier, productPlanIdentifier),
        )
        putString(KEY_STORE, store.stringValue)
        putString(KEY_OWNERSHIP_TYPE, ownershipType.dimensionValue)
        putString(KEY_PERIOD_TYPE, periodType.dimensionValue)
        putDate(KEY_LATEST_PURCHASED_AT, latestPurchaseDate)
        putDate(KEY_ORIGINAL_PURCHASED_AT, originalPurchaseDate)
        putDate(KEY_EXPIRES_AT, expirationDate)
        putDate(KEY_UNSUBSCRIBE_DETECTED_AT, unsubscribeDetectedAt)
        putDate(KEY_BILLING_ISSUE_DETECTED_AT, billingIssueDetectedAt)
        putBool(KEY_IS_SANDBOX, isSandbox)
        putBool(KEY_IS_ACTIVE, isActive)
        putBool(KEY_WILL_RENEW, willRenew)
    }

    internal companion object {
        const val KEY_APP_USER_ID = "appUserId"
        const val KEY_ENTITLEMENTS = "entitlements"
        const val KEY_FIRST_SEEN_AT = "firstSeenAt"
        const val KEY_LAST_SEEN_AT = "lastSeenAt"
        const val KEY_ORIGINAL_APP_USER_ID = "originalAppUserId"
        const val KEY_PURCHASES = "purchases"

        const val KEY_AUTO_RESUME_AT = "autoResumeAt"
        const val KEY_BILLING_ISSUE_DETECTED_AT = "billingIssueDetectedAt"
        const val KEY_DISPLAY_NAME = "displayName"
        const val KEY_EXPIRES_AT = "expiresAt"
        const val KEY_GRACE_PERIOD_EXPIRES_AT = "gracePeriodExpiresAt"
        const val KEY_IDENTIFIER = "identifier"
        const val KEY_IS_ACTIVE = "isActive"
        const val KEY_IS_IN_GRACE_PERIOD = "isInGracePeriod"
        const val KEY_IS_PAUSED = "isPaused"
        const val KEY_IS_REFUNDED = "isRefunded"
        const val KEY_IS_SANDBOX = "isSandbox"
        const val KEY_KIND = "kind"
        const val KEY_LATEST_PURCHASED_AT = "latestPurchasedAt"
        const val KEY_ORIGINAL_PURCHASED_AT = "originalPurchasedAt"
        const val KEY_OWNERSHIP_TYPE = "ownershipType"
        const val KEY_PERIOD_TYPE = "periodType"
        const val KEY_PRICE_AMOUNT_MICROS = "priceAmountMicros"
        const val KEY_PRICE_CURRENCY = "priceCurrency"
        const val KEY_PRODUCT_IDENTIFIER = "productIdentifier"
        const val KEY_PRODUCT_PLAN_IDENTIFIER = "productPlanIdentifier"
        const val KEY_PURCHASED_AT = "purchasedAt"
        const val KEY_PURCHASED_PRODUCT_IDENTIFIER = "purchasedProductIdentifier"
        const val KEY_REFUNDED_AT = "refundedAt"
        const val KEY_STATUS = "status"
        const val KEY_STORE = "store"
        const val KEY_STORE_TRANSACTION_ID = "storeTransactionId"
        const val KEY_TRANSACTION_IDENTIFIER = "transactionIdentifier"
        const val KEY_UNSUBSCRIBE_DETECTED_AT = "unsubscribeDetectedAt"
        const val KEY_WILL_RENEW = "willRenew"

        const val KIND_NON_SUBSCRIPTION = "nonSubscription"
        const val KIND_SUBSCRIPTION = "subscription"

        const val STATUS_ACTIVE = "active"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_IN_GRACE_PERIOD = "in_grace_period"
        const val STATUS_PAUSED = "paused"
        const val STATUS_TRIALING = "trialing"

        /**
         * The base plan is part of what was bought on Google, and it is the form the dashboard lists a
         * subscription under, so it is spelled out as its own value rather than left for a predicate to
         * concatenate. Built the same way [CustomerInfo.allPurchasedProductIds] builds its keys.
         */
        private fun purchasedProductIdentifier(productIdentifier: String, productPlanIdentifier: String?): String =
            productPlanIdentifier
                ?.takeIf { it.isNotEmpty() }
                ?.let { plan -> "$productIdentifier${Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR}$plan" }
                ?: productIdentifier

        /**
         * Not a value to compare against: an unknown ownership type is the absence of one.
         */
        private val OwnershipType.dimensionValue: String?
            get() = when (this) {
                OwnershipType.PURCHASED -> "PURCHASED"
                OwnershipType.FAMILY_SHARED -> "FAMILY_SHARED"
                OwnershipType.UNKNOWN -> null
            }

        /**
         * Where the customer is in this subscription's lifecycle, as one value instead of a combination every
         * predicate has to assemble for itself.
         *
         * Read most specific first: a paused subscription is paused whatever its dates say, a billing issue the
         * store is still serving through outranks the trial or the renewal it interrupted, and anything not
         * currently granting access has expired.
         *
         * `in_billing_retry` and `incomplete` belong to the same vocabulary but are never reported here. Once a
         * grace period is over, whether the store is still retrying is something it tells the backend and not this
         * device, which only sees that access lapsed; a predicate that needs the distinction reads
         * [KEY_BILLING_ISSUE_DETECTED_AT] itself.
         */
        private fun SubscriptionInfo.status(date: Date): String = when {
            autoResumeDate != null -> STATUS_PAUSED
            isInGracePeriod(date) -> STATUS_IN_GRACE_PERIOD
            isActive && periodType == PeriodType.TRIAL -> STATUS_TRIALING
            isActive -> STATUS_ACTIVE
            else -> STATUS_EXPIRED
        }

        /**
         * The store keeps serving a subscription while a billing issue is being retried. Shared by
         * [KEY_IS_IN_GRACE_PERIOD] and [KEY_STATUS] so the two always agree.
         */
        private fun SubscriptionInfo.isInGracePeriod(date: Date): Boolean =
            gracePeriodExpiresDate?.after(date) == true

        private val PeriodType.dimensionValue: String
            get() = when (this) {
                PeriodType.NORMAL -> "normal"
                PeriodType.INTRO -> "intro"
                PeriodType.TRIAL -> "trial"
                PeriodType.PREPAID -> "prepaid"
            }

        private fun Map<String, RulesDimensionValue>.dateOrNull(key: String): Date? =
            (this[key] as? RulesDimensionValue.DateValue)?.value

        private fun MutableMap<String, RulesDimensionValue>.putBool(key: String, value: Boolean) {
            put(key, RulesDimensionValue.BoolValue(value))
        }

        private fun MutableMap<String, RulesDimensionValue>.putString(key: String, value: String?) {
            if (!value.isNullOrEmpty()) put(key, RulesDimensionValue.StringValue(value))
        }

        private fun MutableMap<String, RulesDimensionValue>.putDate(key: String, value: Date?) {
            if (value != null) put(key, RulesDimensionValue.DateValue(value))
        }

        private fun MutableMap<String, RulesDimensionValue>.putObjectList(
            key: String,
            value: List<Map<String, RulesDimensionValue>>,
        ) {
            put(key, RulesDimensionValue.ObjectListValue(value))
        }

        /**
         * The formatted price is deliberately left out: it is rendered in the device's locale, so it is not
         * something a predicate authored once can compare against.
         */
        private fun MutableMap<String, RulesDimensionValue>.putPrice(price: Price?) {
            if (price == null) return
            put(KEY_PRICE_AMOUNT_MICROS, RulesDimensionValue.IntValue(price.amountMicros))
            putString(KEY_PRICE_CURRENCY, price.currencyCode)
        }
    }
}
