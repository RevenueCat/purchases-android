package com.revenuecat.purchases

import com.revenuecat.purchases.util.Iso8601Utils
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections.emptyMap
import java.util.Date

/**
 * Builds a PurchaserInfo
 * @throws [JSONException] If the json is invalid.
 */
@Throws(JSONException::class)
internal fun JSONObject.buildPurchaserInfo(): PurchaserInfo {
    val subscriber = getJSONObject("subscriber")

    val nonSubscriptions = subscriber.getJSONObject("non_subscriptions")
    val nonSubscriptionsLatestPurchases = JSONObject()
    nonSubscriptions.keys().forEach { productId ->
        val arrayOfPurchases = nonSubscriptions.getJSONArray(productId)
        if (arrayOfPurchases.length() > 0) {
            nonSubscriptionsLatestPurchases.put(productId, arrayOfPurchases.getJSONObject(arrayOfPurchases.length() - 1))
        }
    }

    val subscriptions = subscriber.getJSONObject("subscriptions")
    val expirationDatesByProduct = subscriptions.parseExpirations()
    val purchaseDatesByProduct = subscriptions.parsePurchaseDates() + nonSubscriptionsLatestPurchases.parsePurchaseDates()

    var entitlements = JSONObject()
    if (subscriber.has("entitlements")) {
        entitlements = subscriber.getJSONObject("entitlements")
    }
    val expirationDatesByEntitlement = entitlements.parseExpirations()
    val purchaseDatesByEntitlement = entitlements.parsePurchaseDates()

    val requestDate= Iso8601Utils.parse(getString("request_date"))

    val firstSeen = Iso8601Utils.parse(subscriber.getString("first_seen"))

    val entitlementInfos = if (subscriber.has("entitlements")) {
        subscriber.getJSONObject("entitlements").buildEntitlementInfos(subscriptions, nonSubscriptionsLatestPurchases, requestDate)
    } else {
        EntitlementInfos(emptyMap())
    }

    return PurchaserInfo(
        entitlementInfos,
        nonSubscriptions.keys().asSequence().toSet(),
        expirationDatesByProduct,
        purchaseDatesByProduct,
        expirationDatesByEntitlement,
        purchaseDatesByEntitlement,
        requestDate,
        this,
        optInt("schema_version"),
        firstSeen,
        subscriber.optString("original_app_user_id")
    )
}

private fun JSONObject.buildEntitlementInfos(
    subscriptions: JSONObject,
    nonSubscriptionsLatestPurchases: JSONObject,
    requestDate: Date?
): EntitlementInfos {
    val all = mutableMapOf<String, EntitlementInfo>()
    keys().forEach { entitlementId ->
        val entitlement = getJSONObject(entitlementId)
        entitlement.optString("product_identifier").takeIf { it.isNotEmpty() }?.let { productIdentifier ->
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

private fun JSONObject.optDate(name: String) =
    takeUnless { isNull(name) }?.getString(name)?.let {
        Iso8601Utils.parse(it)
    }

private fun JSONObject.getDate(name: String) = Iso8601Utils.parse(getString(name))

private fun JSONObject.getStore(name: String) = when(getString(name)) {
    "app_store" -> Store.APP_STORE
    "mac_app_store" -> Store.MAC_APP_STORE
    "play_store" -> Store.PLAY_STORE
    "stripe" -> Store.STRIPE
    "promotional" -> Store.PROMOTIONAL
    else -> Store.UNKNOWN_STORE
}

private fun JSONObject.optPeriodType(name: String) = when(optString(name)) {
    "normal" -> PeriodType.NORMAL
    "intro" -> PeriodType.INTRO
    "trial" -> PeriodType.TRIAL
    else -> PeriodType.NORMAL
}

private fun JSONObject.buildEntitlementInfo(
    identifier: String,
    productData: JSONObject,
    requestDate: Date?
): EntitlementInfo {
    val expirationDate = optDate("expires_date")
    val unsubscribeDetectedAt = productData.optDate("unsubscribe_detected_at")
    val billingIssueDetectedAt = productData.optDate("billing_issues_detected_at")
    return EntitlementInfo(
        identifier = identifier,
        isActive = expirationDate == null || expirationDate.after(requestDate ?: Date()),
        willRenew = expirationDate == null || (unsubscribeDetectedAt == null && billingIssueDetectedAt == null),
        periodType = productData.optPeriodType("period_type"),
        latestPurchaseDate = getDate("purchase_date"),
        originalPurchaseDate = productData.getDate("original_purchase_date"),
        expirationDate = expirationDate,
        store = productData.getStore("store"),
        productIdentifier = getString("product_identifier"),
        isSandbox = productData.getBoolean("is_sandbox"),
        unsubscribeDetectedAt = unsubscribeDetectedAt,
        billingIssueDetectedAt = billingIssueDetectedAt
    )
}

internal fun JSONObject.buildEntitlementsMap(): Map<String, Entitlement> {
    return mapKeyAndContent { parseEntitlement() }
}

private fun JSONObject.parseActiveProductIdentifier() =
    Offering(getString("active_product_identifier"))

private fun JSONObject.parseEntitlement(): Entitlement {
    return Entitlement(
        getJSONObject("offerings").mapKeyAndContent { parseActiveProductIdentifier() }
    )
}

private fun <T> JSONObject.mapKeyAndContent(transformation: JSONObject.() -> T): Map<String, T> {
    return keys().asSequence().map { jsonName ->
        try {
            jsonName to transformation.invoke(getJSONObject(jsonName))
        } catch (ignored: JSONException) {
            null
        }
    }.filterNotNull().toMap()
}
