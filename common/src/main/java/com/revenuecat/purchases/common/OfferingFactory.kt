package com.revenuecat.purchases.common

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONObject

abstract class OfferingFactory {

    abstract fun Map<String, List<StoreProduct>>.findMatchingProduct(
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct?

    /**
     * Note: this may return an empty Offerings.
     */
    fun createOfferings(offeringsJson: JSONObject, productsById: Map<String, List<StoreProduct>>): Offerings {
        val jsonOfferings = offeringsJson.getJSONArray("offerings")
        val currentOfferingID = offeringsJson.getString("current_offering_id")

        val offerings = mutableMapOf<String, Offering>()
        for (i in 0 until jsonOfferings.length()) {
            val offeringJson = jsonOfferings.getJSONObject(i)
            createOffering(offeringJson, productsById)?.let {
                offerings[it.identifier] = it

                if (it.availablePackages.isEmpty()) {
                    warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
                }
            }
        }

        return Offerings(offerings[currentOfferingID], offerings)
    }

    private fun createOffering(offeringJson: JSONObject, productsById: Map<String, List<StoreProduct>>): Offering? {
        val offeringIdentifier = offeringJson.getString("identifier")
        val jsonPackages = offeringJson.getJSONArray("packages")

        val availablePackages = mutableListOf<Package>()
        for (i in 0 until jsonPackages.length()) {
            val packageJson = jsonPackages.getJSONObject(i)
            createPackage(packageJson, productsById, offeringIdentifier)?.let {
                availablePackages.add(it)
            }
        }

        return if (availablePackages.isNotEmpty()) {
            Offering(offeringIdentifier, offeringJson.getString("description"), availablePackages)
        } else {
            null
        }
    }

    private fun createPackage(
        packageJson: JSONObject,
        productsById: Map<String, List<StoreProduct>>,
        offeringIdentifier: String
    ): Package? {
        val packageIdentifier = packageJson.getString("identifier")
        val productIdentifier = packageJson.getString("platform_product_identifier")

        val planIdentifier = packageJson.optString("platform_product_plan_identifier").takeIf { it.isNotEmpty() }
        val product = productsById.findMatchingProduct(productIdentifier, planIdentifier)

        val packageType = packageIdentifier.toPackageType()
        return product?.let { Package(packageIdentifier, packageType, it, offeringIdentifier) }
    }
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
