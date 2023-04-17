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

    val entitlementPurchaseDate = getDate(ProductResponseJsonKeys.PURCHASE_DATE)

    return EntitlementInfo(
        identifier = identifier,
        isActive = isDateActive(identifier, expirationDate, requestDate),
        willRenew = getWillRenew(store, expirationDate, unsubscribeDetectedAt,
            billingIssueDetectedAt),
        periodType = productData.optPeriodType(ProductResponseJsonKeys.PERIOD_TYPE),
        latestPurchaseDate = entitlementPurchaseDate,
        originalPurchaseDate = this.calculateOriginalPurchaseDate(productData),
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

// If a product's base plans grants different entitlements, the product data section of the customerInfo response
// might not match the information for the entitlement. If will have the latest purchased plan's information only.
// This means we only have the original_purchase_date for the latest base plan purchased.
//
// For example:
//
// An entitlement could have this response from the backend (note the purchase is for p1m):
// {
//   "expires_date":"2023-04-11T17:59:03.285Z",
//   "product_identifier":"prod_1",
//   "purchase_date":"2023-04-11T16:59:03.285Z",
//   "product_plan_identifier":"p1m"
// }
//
// But the product data could be for a more recent purchase for another plan (not_bw):
// {
//   "billing_issues_detected_at":null,
//   "is_sandbox":false,
//   "original_purchase_date":"2023-04-11T17:59:03.285Z",
//   "purchase_date":"2023-04-11T17:59:03.285Z",
//   "store":"play_store",
//   "unsubscribe_detected_at":null,
//   "product_plan_identifier":"not_bw",
//   "expires_date":"2023-04-12T18:59:01.115Z",
//   "period_type":"normal"
// }
private fun JSONObject.calculateOriginalPurchaseDate(productData: JSONObject): Date =
    if (productData.optNullableString(ProductResponseJsonKeys.PRODUCT_PLAN_IDENTIFIER) ==
        this.optNullableString(EntitlementsResponseJsonKeys.PRODUCT_PLAN_IDENTIFIER)) {
        productData.getDate(ProductResponseJsonKeys.ORIGINAL_PURCHASE_DATE)
    } else {
        this.getDate(EntitlementsResponseJsonKeys.PURCHASE_DATE)
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
