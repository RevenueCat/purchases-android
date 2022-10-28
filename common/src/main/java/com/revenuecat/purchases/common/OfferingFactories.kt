package com.revenuecat.purchases.common

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONObject

/**
 * Note: this may return an empty Offerings.
 */
fun JSONObject.createOfferings(productsById: Map<String, List<StoreProduct>>): Offerings {
    val jsonOfferings = getJSONArray("offerings")
    val currentOfferingID = getString("current_offering_id")

    val offerings = mutableMapOf<String, Offering>()
    for (i in 0 until jsonOfferings.length()) {
        val offeringJson = jsonOfferings.getJSONObject(i)
        offeringJson.createOffering(productsById)?.let {
            offerings[it.identifier] = it

            if (it.availablePackages.isEmpty()) {
                warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
            }
        }
    }

    return Offerings(offerings[currentOfferingID], offerings)
}

fun JSONObject.createOffering(productsById: Map<String, List<StoreProduct>>): Offering? {
    val offeringIdentifier = getString("identifier")
    val jsonPackages = getJSONArray("packages")

    val availablePackages = mutableListOf<Package>()
    for (i in 0 until jsonPackages.length()) {
        val packageJson = jsonPackages.getJSONObject(i)
        packageJson.createPackage(productsById, offeringIdentifier)?.let {
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
    offeringIdentifier: String
): Package? {
    val packageIdentifier = getString("identifier")
    val productGroupIdentifier = optString("platform_product_group_identifier")
    val productIdentifier = getString("platform_product_identifier")
    val productDuration = optString("product_duration")
    return productsById[productGroupIdentifier]?.let { product ->
        val packageType = packageIdentifier.toPackageType()
        return Package(packageIdentifier, packageType, product.first(), offeringIdentifier, productDuration, productIdentifier)
    }
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
