package com.revenuecat.purchases.common

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONObject

/**
 * Note: this may return an empty Offerings.
 */
fun JSONObject.createOfferings(productsById: Map<String, List<StoreProduct>>, store: Store): Offerings {
    val jsonOfferings = getJSONArray("offerings")
    val currentOfferingID = getString("current_offering_id")

    val offerings = mutableMapOf<String, Offering>()
    for (i in 0 until jsonOfferings.length()) {
        val offeringJson = jsonOfferings.getJSONObject(i)
        offeringJson.createOffering(productsById, store)?.let {
            offerings[it.identifier] = it

            if (it.availablePackages.isEmpty()) {
                warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
            }
        }
    }

    return Offerings(offerings[currentOfferingID], offerings)
}

fun JSONObject.createOffering(productsById: Map<String, List<StoreProduct>>, store: Store): Offering? {
    val offeringIdentifier = getString("identifier")
    val jsonPackages = getJSONArray("packages")

    val availablePackages = mutableListOf<Package>()
    for (i in 0 until jsonPackages.length()) {
        val packageJson = jsonPackages.getJSONObject(i)
        packageJson.createPackage(productsById, offeringIdentifier, store)?.let {
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
    productsById: Map<String, List<StoreProduct>>,
    offeringIdentifier: String,
    store: Store
): Package? {
    val packageIdentifier = getString("identifier")
    val productIdentifier = getString("platform_product_identifier")

    val product = if (store == Store.PLAY_STORE) {
        val planIdentifier = optString("platform_product_plan_identifier").takeIf { it.isNotEmpty() }
        getMatchingGoogleProduct(productsById, productIdentifier, planIdentifier)
    } else {
        productsById[productIdentifier]?.get(0)
    }

    val packageType = packageIdentifier.toPackageType()
    return product?.let { Package(packageIdentifier, packageType, it, offeringIdentifier) }
}

private fun getMatchingGoogleProduct(
    productsById: Map<String, List<StoreProduct>>,
    productIdentifier: String,
    planIdentifier: String?
): StoreProduct? {
    if (planIdentifier == null) {
        // It could be an INAPP or a mis-configured subscription
        // Try to find INAPP, otherwise null
        return productsById[productIdentifier]
            .takeIf { it?.size == 1 }
            ?.takeIf { it[0].type == ProductType.INAPP }
            ?.get(0)
    }
    val storeProducts: List<StoreProduct>? = productsById[productIdentifier]
    return storeProducts?.firstOrNull { storeProduct ->
        storeProduct.subscriptionOptions.firstOrNull { it.isBasePlan }?.id == planIdentifier
    }
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
