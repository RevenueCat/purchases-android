package com.revenuecat.purchases

import com.revenuecat.purchases.util.Iso8601Utils
import org.json.JSONException
import org.json.JSONObject

/**
 * Builds a PurchaserInfo
 * @throws [JSONException] If the json is invalid.
 */
@Throws(JSONException::class)
internal fun JSONObject.buildPurchaserInfo(): PurchaserInfo {
    val subscriber = getJSONObject("subscriber")

    val otherPurchases = subscriber.getJSONObject("other_purchases")
    val nonSubscriptionPurchases = otherPurchases.keys().asSequence().toSet()
    val subscriptions = subscriber.getJSONObject("subscriptions")
    val expirationDatesByProduct = subscriptions.parseExpirations()
    val purchaseDatesByProduct = subscriptions.parsePurchaseDates()

    var entitlements = JSONObject()
    if (subscriber.has("entitlements")) {
        entitlements = subscriber.getJSONObject("entitlements")
    }

    val expirationDatesByEntitlement = entitlements.parseExpirations()
    val purchaseDatesByEntitlement = entitlements.parsePurchaseDates()

    val requestDate =
        if (has("request_date")) {
            try {
                getString("request_date").takeUnless { it.isNullOrBlank() }
                    ?.let {
                        Iso8601Utils.parse(it)
                    }
            } catch (e: RuntimeException) {
                throw JSONException(e.localizedMessage)
            }
        } else null

    return PurchaserInfo(
        nonSubscriptionPurchases,
        expirationDatesByProduct,
        purchaseDatesByProduct,
        expirationDatesByEntitlement,
        purchaseDatesByEntitlement,
        requestDate,
        this,
        optInt("schema_version")
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
