package com.revenuecat.purchases.common

import android.net.Uri
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.getDate
import com.revenuecat.purchases.utils.optDate
import com.revenuecat.purchases.utils.optNullableString
import com.revenuecat.purchases.utils.parseExpirations
import com.revenuecat.purchases.utils.parsePurchaseDates
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections.emptyMap
import java.util.Date

/**
 * Builds a PurchaserInfo
 * @throws [JSONException] If the json is invalid.
 */
@Throws(JSONException::class)
fun JSONObject.buildPurchaserInfo(): PurchaserInfo {
    val subscriber = getJSONObject("subscriber")

    val nonSubscriptions = subscriber.getJSONObject("non_subscriptions")
    val nonSubscriptionsLatestPurchases = JSONObject()
    nonSubscriptions.keys().forEach { productId ->
        val arrayOfNonSubscriptions = nonSubscriptions.getJSONArray(productId)
        val numberOfNonSubscriptions = arrayOfNonSubscriptions.length()
        if (numberOfNonSubscriptions > 0) {
            nonSubscriptionsLatestPurchases.put(
                productId,
                arrayOfNonSubscriptions.getJSONObject(numberOfNonSubscriptions - 1)
            )
        }
    }

    val subscriptions = subscriber.getJSONObject("subscriptions")
    val expirationDatesByProduct = subscriptions.parseExpirations()
    val purchaseDatesByProduct =
        subscriptions.parsePurchaseDates() + nonSubscriptionsLatestPurchases.parsePurchaseDates()

    val entitlements = subscriber.optJSONObject("entitlements")

    val requestDate = Iso8601Utils.parse(getString("request_date"))

    val firstSeen = Iso8601Utils.parse(subscriber.getString("first_seen"))

    val entitlementInfos =
        entitlements?.buildEntitlementInfos(subscriptions, nonSubscriptionsLatestPurchases, requestDate)
            ?: EntitlementInfos(emptyMap())

    val managementURL = subscriber.optNullableString("management_url")
    val originalPurchaseDate = subscriber.optNullableString("original_purchase_date")?.let {
        Iso8601Utils.parse(it) ?: null
    }

    return PurchaserInfo(
        entitlements = entitlementInfos,
        purchasedNonSubscriptionSkus = nonSubscriptions.keys().asSequence().toSet(),
        allExpirationDatesByProduct = expirationDatesByProduct,
        allPurchaseDatesByProduct = purchaseDatesByProduct,
        requestDate = requestDate,
        jsonObject = this,
        schemaVersion = optInt("schema_version"),
        firstSeen = firstSeen,
        originalAppUserId = subscriber.optString("original_app_user_id"),
        managementURL = managementURL?.let { Uri.parse(it) },
        originalPurchaseDate = originalPurchaseDate
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

private fun JSONObject.getStore(name: String) = when (getString(name)) {
    "app_store" -> Store.APP_STORE
    "mac_app_store" -> Store.MAC_APP_STORE
    "play_store" -> Store.PLAY_STORE
    "stripe" -> Store.STRIPE
    "promotional" -> Store.PROMOTIONAL
    "amazon_store" -> Store.AMAZON
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

fun JSONObject.createOfferings(products: Map<String, ProductDetails>): Offerings {
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

fun JSONObject.createOffering(products: Map<String, ProductDetails>): Offering? {
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

fun JSONObject.createPackage(
    products: Map<String, ProductDetails>,
    offeringIdentifier: String
): Package? {
    return products[getString("platform_product_identifier")]?.let { product ->
        val identifier = getString("identifier")
        val packageType = identifier.toPackageType()
        return Package(identifier, packageType, product, offeringIdentifier)
    }
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
