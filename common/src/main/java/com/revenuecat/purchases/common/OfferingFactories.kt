package com.revenuecat.purchases.common

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreProductImpl
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONObject

val FAKE_PRODUCT = StoreProductImpl(
    "SKU",
    ProductType.SUBS,
    "",
    1,
    "USD",
    "originalPrice",
    100,
    "",
    "",
    "P1M",
    null,
    null,
    0,
    null,
    0,
    "icon",
    JSONObject("{}"),
)

/**
 * Note: this may return an empty Offerings.
 */
fun JSONObject.createOfferings(): Offerings {
    val jsonOfferings = getJSONArray("offerings")
    val currentOfferingID = getString("current_offering_id")

    val offerings = mutableMapOf<String, Offering>()
    for (i in 0 until jsonOfferings.length()) {
        val offeringJson = jsonOfferings.getJSONObject(i)
        offeringJson.createOffering()?.let {
            offerings[it.identifier] = it

            if (it.availablePackages.isEmpty()) {
                warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
            }
        }
    }

    return Offerings(offerings[currentOfferingID], offerings)
}

fun JSONObject.createOffering(): Offering? {
    val offeringIdentifier = getString("identifier")
    val jsonPackages = getJSONArray("packages")

    val availablePackages = mutableListOf<Package>()
    for (i in 0 until jsonPackages.length()) {
        val packageJson = jsonPackages.getJSONObject(i)
        availablePackages.add(packageJson.createPackage(offeringIdentifier))
    }

    return if (availablePackages.isNotEmpty()) {
        Offering(offeringIdentifier, getString("description"), availablePackages)
    } else {
        null
    }
}

fun JSONObject.createPackage(
    offeringIdentifier: String
): Package {
    val product_identifier = getString("platform_product_identifier")
    val identifier = getString("identifier")
    val group_identifier = optionalString("platform_product_group_identifier")
    val duration = optionalString("product_duration")
    val packageType = identifier.toPackageType()
    return Package(identifier, packageType, FAKE_PRODUCT, offeringIdentifier, duration, product_identifier, group_identifier)
}


fun JSONObject.optionalString(id: String): String? {
    if (this.has(id)) {
        return getString(id)
    }
    return null
}

fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
