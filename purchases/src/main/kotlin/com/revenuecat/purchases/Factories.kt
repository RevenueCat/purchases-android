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
    val expirationDatesByProduct = subscriptions.parseExpirations() + nonSubscriptionsLatestPurchases.parseExpirations()
    val purchaseDatesByProduct = subscriptions.parsePurchaseDates() + nonSubscriptionsLatestPurchases.parsePurchaseDates()

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

    val firstSeen =
        if (has("first_seen")) {
            try {
                getString("first_seen").takeUnless { it.isNullOrBlank() }
                    ?.let {
                        Iso8601Utils.parse(it)
                    }
            } catch (e: RuntimeException) {
                throw JSONException(e.localizedMessage)
            }
        } else null

    val entitlements = if (subscriber.has("entitlements")) {
        subscriber.getJSONObject("entitlements").buildEntitlementInfos(subscriptions, nonSubscriptionsLatestPurchases, requestDate)
    } else {
        EntitlementInfos(emptyMap())
    }

    return PurchaserInfo(
        entitlements,
        nonSubscriptions.keys().asSequence().toSet(),
        expirationDatesByProduct,
        purchaseDatesByProduct,
        requestDate,
        this,
        optInt("schema_version"),
        firstSeen,
        optString("original_app_user_id")
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
        if (entitlement.has("product_identifier")) {
            val productIdentifier = entitlement.getString("product_identifier")
            // TODO: is it possible that consumable and subscription have the same ID?
            if (subscriptions.has(productIdentifier)) {
                all[entitlementId] = subscriptions.getJSONObject(productIdentifier).buildEntitlementInfo(productIdentifier, requestDate)
            } else if (nonSubscriptionsLatestPurchases.has(productIdentifier)) {
                all[entitlementId] = nonSubscriptionsLatestPurchases.getJSONObject(productIdentifier).buildEntitlementInfo(productIdentifier, requestDate)
            }
        }

    }
    return EntitlementInfos(all)
}

private fun JSONObject.buildEntitlementInfo(
    identifier: String,
    requestDate: Date?
): EntitlementInfo {
    val expirationDate = Iso8601Utils.parse(getString("expires_date"))
    val unsubscribeDetectedAt = Iso8601Utils.parse(getString("unsubscribe_detected_at"))
    val billingIssueDetectedAt = Iso8601Utils.parse(getString("unsubscribe_detected_at"))
    return EntitlementInfo(
        identifier,
        expirationDate == null || expirationDate.after(requestDate ?: Date()),
        expirationDate == null || (unsubscribeDetectedAt == null && billingIssueDetectedAt == null),
        getString("period_type").parsePeriodType(),
        Iso8601Utils.parse(getString("purchase_date")),
        Iso8601Utils.parse(getString("original_purchase_date")),
        Iso8601Utils.parse(getString("expires_date")),
        getString("store").parseStore(),
        getString("product_identifier"),
        getBoolean("is_sandbox"),
        unsubscribeDetectedAt, billingIssueDetectedAt
    )
}

private fun String.parseStore(): Store {
    return when(this) {
        "app_store" -> Store.APP_STORE
        "mac_app_store" -> Store.MAC_APP_STORE
        "play_store" -> Store.PLAY_STORE
        "stripe" -> Store.STRIPE
        "promotional" -> Store.PROMOTIONAL
        else -> Store.UNKNOWN_STORE
    }
}

private fun String.parsePeriodType(): PeriodType {
    return when(this) {
        "normal" -> PeriodType.NORMAL
        "intro" -> PeriodType.INTRO
        "trial" -> PeriodType.TRIAL
        else -> PeriodType.NORMAL
    }
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
