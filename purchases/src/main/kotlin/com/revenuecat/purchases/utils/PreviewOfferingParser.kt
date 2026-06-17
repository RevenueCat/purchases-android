package com.revenuecat.purchases.utils

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

/**
 * This is instantiated via reflection by revenuecatui, in `TemplatePreviews.kt`, to be able to show previews of entire
 * v2 Paywall templates.
 */
@Suppress("UnusedPrivateClass", "unused")
private class PreviewOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        // Ignoring productsById and just returning a product based on the identifier in the packageJson.
        val identifier = packageJson.getString("identifier")
        val packageType = PackageType.values().first { packageType -> packageType.identifier == identifier }

        return defaultTestStoreProduct(packageType)
    }
}
