package com.revenuecat.purchases.common

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.utils.getDate
import com.revenuecat.purchases.utils.optDate
import org.json.JSONObject
import java.util.Date

internal fun JSONObject.buildEntitlementInfos(
    subscriptions: JSONObject,
    nonSubscriptionsLatestPurchases: JSONObject,
    requestDate: Date?
): EntitlementInfos {
    val all = mutableMapOf<String, EntitlementInfo>()
    keys().forEach { entitlementId ->
        val entitlement = getJSONObject(entitlementId)
        entitlement.optString("product_identifier").takeIf { it.isNotEmpty() }
            ?.let { productIdentifier ->
                if (subscriptions.has(productIdentifier)) {
                    all[entitlementId] = entitlement.buildEntitlementInfo(
                        entitlementId,
                        subscriptions.getJSONObject(productIdentifier),
                        requestDate
                    )
                } else if (nonSubscriptionsLatestPurchases.has(productIdentifier)) {
                    all[entitlementId] = entitlement.buildEntitlementInfo(
                        entitlementId,
                        nonSubscriptionsLatestPurchases.getJSONObject(productIdentifier),
                        requestDate
                    )
                }
            }
    }
    return EntitlementInfos(all)
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

internal fun JSONObject.buildEntitlementInfo(
    identifier: String,
    productData: JSONObject,
    requestDate: Date?
): EntitlementInfo {
    val expirationDate = optDate("expires_date")
    val unsubscribeDetectedAt = productData.optDate("unsubscribe_detected_at")
    val billingIssueDetectedAt = productData.optDate("billing_issues_detected_at")

    val store = productData.getStore("store")

    return EntitlementInfo(
        identifier = identifier,
        isActive = expirationDate == null || expirationDate.after(requestDate ?: Date()),
        willRenew = getWillRenew(store, expirationDate, unsubscribeDetectedAt,
            billingIssueDetectedAt),
        periodType = productData.optPeriodType("period_type"),
        latestPurchaseDate = getDate("purchase_date"),
        originalPurchaseDate = productData.getDate("original_purchase_date"),
        expirationDate = expirationDate,
        store = store,
        productIdentifier = getString("product_identifier"),
        isSandbox = productData.getBoolean("is_sandbox"),
        unsubscribeDetectedAt = unsubscribeDetectedAt,
        billingIssueDetectedAt = billingIssueDetectedAt,
        ownershipType = productData.optOwnershipType("ownership_type")
    )
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
