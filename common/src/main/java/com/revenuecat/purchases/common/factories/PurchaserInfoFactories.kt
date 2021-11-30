package com.revenuecat.purchases.common.factories

import android.net.Uri
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.optNullableString
import com.revenuecat.purchases.utils.parseExpirations
import com.revenuecat.purchases.utils.parsePurchaseDates
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections.emptyMap

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
        return Package(identifier, packageType, product.skuDetails, offeringIdentifier)
    }
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
