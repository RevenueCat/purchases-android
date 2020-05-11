package com.revenuecat.purchases

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.attributes.SubscriberAttribute
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
            nonSubscriptionsLatestPurchases.put(
                productId,
                arrayOfPurchases.getJSONObject(arrayOfPurchases.length() - 1)
            )
        }
    }

    val subscriptions = subscriber.getJSONObject("subscriptions")
    val expirationDatesByProduct = subscriptions.parseExpirations()
    val purchaseDatesByProduct =
        subscriptions.parsePurchaseDates() + nonSubscriptionsLatestPurchases.parsePurchaseDates()

    var entitlements = JSONObject()
    if (subscriber.has("entitlements")) {
        entitlements = subscriber.getJSONObject("entitlements")
    }
    val expirationDatesByEntitlement = entitlements.parseExpirations()
    val purchaseDatesByEntitlement = entitlements.parsePurchaseDates()

    val requestDate = Iso8601Utils.parse(getString("request_date"))

    val firstSeen = Iso8601Utils.parse(subscriber.getString("first_seen"))

    val entitlementInfos = if (subscriber.has("entitlements")) {
        subscriber.getJSONObject("entitlements")
            .buildEntitlementInfos(subscriptions, nonSubscriptionsLatestPurchases, requestDate)
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

private fun JSONObject.optDate(name: String) =
    takeUnless { isNull(name) }?.getString(name)?.let {
        Iso8601Utils.parse(it)
    }

private fun JSONObject.getDate(name: String) = Iso8601Utils.parse(getString(name))

private fun JSONObject.getStore(name: String) = when (getString(name)) {
    "app_store" -> Store.APP_STORE
    "mac_app_store" -> Store.MAC_APP_STORE
    "play_store" -> Store.PLAY_STORE
    "stripe" -> Store.STRIPE
    "promotional" -> Store.PROMOTIONAL
    else -> Store.UNKNOWN_STORE
}

private fun JSONObject.optPeriodType(name: String) = when (optString(name)) {
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

internal fun JSONObject.createOfferings(products: Map<String, SkuDetails>): Offerings {
    val jsonOfferings = getJSONArray("offerings")
    val currentOfferingID = getString("current_offering_id")

    val offerings = mutableMapOf<String, Offering>()
    for (i in 0 until jsonOfferings.length()) {
        jsonOfferings.getJSONObject(i).createOffering(products)?.let {
            offerings[it.identifier] = it
        }
    }

    return Offerings(offerings[currentOfferingID], offerings)
}

internal fun JSONObject.createOffering(products: Map<String, SkuDetails>): Offering? {
    val offeringIdentifier = getString("identifier")
    val jsonPackages = getJSONArray("packages")

    val availablePackages = mutableListOf<Package>()
    for (i in 0 until jsonPackages.length()) {
        jsonPackages.getJSONObject(i).createPackage(products, offeringIdentifier)?.let {
            availablePackages.add(it)
        }
    }

    return if (availablePackages.isNotEmpty()) {
        Offering(offeringIdentifier, getString("description"), availablePackages)
    } else {
        null
    }
}

internal fun JSONObject.createPackage(
    products: Map<String, SkuDetails>,
    offeringIdentifier: String
): Package? {
    return products[getString("platform_product_identifier")]?.let { product ->
        val identifier = getString("identifier")
        val packageType = identifier.toPackageType()
        return Package(identifier, packageType, product, offeringIdentifier)
    }
}

internal fun JSONObject.buildSubscriberAttributes(): Map<String, SubscriberAttribute> {
    val attributesJSONObject = getJSONObject("attributes")
    return attributesJSONObject.keys().asSequence().map { attributeKey ->
        val attributeJSONObject = attributesJSONObject[attributeKey] as JSONObject
        attributeKey to SubscriberAttribute(attributeJSONObject)
    }.toMap()
}
