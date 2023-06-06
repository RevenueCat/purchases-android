package com.revenuecat.purchases.common

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.responses.EntitlementsResponseJsonKeys
import com.revenuecat.purchases.common.responses.ProductResponseJsonKeys
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.utils.DateHelper
import com.revenuecat.purchases.utils.getDate
import com.revenuecat.purchases.utils.optDate
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONObject
import java.util.Date

internal fun JSONObject.buildEntitlementInfos(
    subscriptions: JSONObject,
    nonSubscriptionsLatestPurchases: JSONObject,
    requestDate: Date,
    verificationResult: VerificationResult
): EntitlementInfos {
    val all = mutableMapOf<String, EntitlementInfo>()
    keys().forEach { entitlementId ->
        val entitlement = getJSONObject(entitlementId)

        val productIdentifier = entitlement.optString(EntitlementsResponseJsonKeys.PRODUCT_IDENTIFIER)
        productIdentifier.takeIf { it.isNotEmpty() }
            ?.let { productIdentifier ->
                if (subscriptions.has(productIdentifier)) {
                    all[entitlementId] = entitlement.buildEntitlementInfo(
                        entitlementId,
                        subscriptions.getJSONObject(productIdentifier),
                        requestDate,
                        verificationResult
                    )
                } else if (nonSubscriptionsLatestPurchases.has(productIdentifier)) {
                    all[entitlementId] = entitlement.buildEntitlementInfo(
                        entitlementId,
                        nonSubscriptionsLatestPurchases.getJSONObject(productIdentifier),
                        requestDate,
                        verificationResult
                    )
                }
            }
    }
    return EntitlementInfos(
        all,
        // Trusted entitlements: Commented out until ready to be made public
        // verificationResult
    )
}

internal fun JSONObject.getStore(name: String) = when (getString(name)) {
    "app_store" -> Store.APP_STORE
    "mac_app_store" -> Store.MAC_APP_STORE
    "play_store" -> Store.PLAY_STORE
    "stripe" -> Store.STRIPE
    "promotional" -> Store.PROMOTIONAL
    "amazon" -> Store.AMAZON
    else -> Store.UNKNOWN_STORE
}

internal fun JSONObject.optPeriodType(name: String) = when (optString(name)) {
    "normal" -> PeriodType.NORMAL
    "intro" -> PeriodType.INTRO
    "trial" -> PeriodType.TRIAL
    else -> PeriodType.NORMAL
}

internal fun JSONObject.optOwnershipType(name: String) = when (optString(name)) {
    "PURCHASED" -> OwnershipType.PURCHASED
    "FAMILY_SHARED" -> OwnershipType.FAMILY_SHARED
    else -> OwnershipType.UNKNOWN
}

// Trusted entitlements: Added until ready to use it.
@Suppress("UnusedPrivateMember")
internal fun JSONObject.buildEntitlementInfo(
    identifier: String,
    productData: JSONObject,
    requestDate: Date,
    verificationResult: VerificationResult
): EntitlementInfo {
    val expirationDate = optDate(EntitlementsResponseJsonKeys.EXPIRES_DATE)
    val unsubscribeDetectedAt = productData.optDate(ProductResponseJsonKeys.UNSUBSCRIBE_DETECTED_AT)
    val billingIssueDetectedAt = productData.optDate(ProductResponseJsonKeys.BILLING_ISSUES_DETECTED_AT)

    val store = productData.getStore(ProductResponseJsonKeys.STORE)

    return EntitlementInfo(
        identifier = identifier,
        isActive = isDateActive(identifier, expirationDate, requestDate),
        willRenew = getWillRenew(
            store,
            expirationDate,
            unsubscribeDetectedAt,
            billingIssueDetectedAt
        ),
        periodType = productData.optPeriodType(ProductResponseJsonKeys.PERIOD_TYPE),
        latestPurchaseDate = getDate(ProductResponseJsonKeys.PURCHASE_DATE),
        originalPurchaseDate = productData.getDate(ProductResponseJsonKeys.ORIGINAL_PURCHASE_DATE),
        expirationDate = expirationDate,
        store = store,
        productIdentifier = getString(EntitlementsResponseJsonKeys.PRODUCT_IDENTIFIER),
        productPlanIdentifier = optNullableString(EntitlementsResponseJsonKeys.PRODUCT_PLAN_IDENTIFIER),
        isSandbox = productData.getBoolean(ProductResponseJsonKeys.IS_SANDBOX),
        unsubscribeDetectedAt = unsubscribeDetectedAt,
        billingIssueDetectedAt = billingIssueDetectedAt,
        ownershipType = productData.optOwnershipType(ProductResponseJsonKeys.OWNERSHIP_TYPE),
        jsonObject = this,
        // Trusted entitlements: Commented out until ready to be made public
        // verificationResult = verificationResult
    )
}

private fun isDateActive(
    identifier: String,
    expirationDate: Date?,
    requestDate: Date
): Boolean {
    val dateActive = DateHelper.isDateActive(expirationDate, requestDate)
    if (!dateActive.isActive && !dateActive.inGracePeriod) {
        warnLog(
            PurchaseStrings.ENTITLEMENT_EXPIRED_OUTSIDE_GRACE_PERIOD.format(identifier, expirationDate, requestDate)
        )
    }
    return dateActive.isActive
}

private fun getWillRenew(
    store: Store,
    expirationDate: Date?,
    unsubscribeDetectedAt: Date?,
    billingIssueDetectedAt: Date?
): Boolean {
    val isPromo = store == Store.PROMOTIONAL
    val isLifetime = expirationDate == null
    val hasUnsubscribed = unsubscribeDetectedAt != null
    val hasBillingIssues = billingIssueDetectedAt != null
    return !(isPromo || isLifetime || hasUnsubscribed || hasBillingIssues)
}
