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
fun JSONObject.createOfferings(
    productsById: Map<String, List<StoreProduct>>,
    productMatchingMethod: (Map<String, List<StoreProduct>>, String, String?) -> StoreProduct?
): Offerings {
    val jsonOfferings = getJSONArray("offerings")
    val currentOfferingID = getString("current_offering_id")

    val offerings = mutableMapOf<String, Offering>()
    for (i in 0 until jsonOfferings.length()) {
        val offeringJson = jsonOfferings.getJSONObject(i)
        offeringJson.createOffering(productsById, productMatchingMethod)?.let {
            offerings[it.identifier] = it

            if (it.availablePackages.isEmpty()) {
                warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
            }
        }
    }

    return Offerings(offerings[currentOfferingID], offerings)
}

fun JSONObject.createOffering(
    productsById: Map<String, List<StoreProduct>>,
    productMatchingMethod: (Map<String, List<StoreProduct>>, String, String?) -> StoreProduct?
): Offering? {
    val offeringIdentifier = getString("identifier")
    val jsonPackages = getJSONArray("packages")

    val availablePackages = mutableListOf<Package>()
    for (i in 0 until jsonPackages.length()) {
        val packageJson = jsonPackages.getJSONObject(i)
        packageJson.createPackage(productsById, offeringIdentifier, productMatchingMethod)?.let {
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
    productMatchingMethod: (Map<String, List<StoreProduct>>, String, String?) -> StoreProduct?
): Package? {
    val packageIdentifier = getString("identifier")
    val productIdentifier = getString("platform_product_identifier")

    val planIdentifier = optString("platform_product_plan_identifier").takeIf { it.isNotEmpty() }
    val product = productMatchingMethod.invoke(productsById, productIdentifier, planIdentifier)

    val packageType = packageIdentifier.toPackageType()
    return product?.let { Package(packageIdentifier, packageType, it, offeringIdentifier) }
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
