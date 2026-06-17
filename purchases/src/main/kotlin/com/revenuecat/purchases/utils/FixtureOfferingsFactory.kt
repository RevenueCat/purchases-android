package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct
import org.json.JSONObject

/**
 * Creates [Offerings] from an offerings-endpoint-shaped JSON object without querying Google Play Billing,
 * substituting [TestStoreProduct]s for real store products.
 *
 * This exists to support offline paywall rendering in tests and tooling (e.g. the paywall snapshot testing
 * kit). Prices are not part of the offerings backend response (they come from Play Billing at runtime), so
 * products are fabricated with fixed default prices unless overridden.
 */
@InternalRevenueCatAPI
public object FixtureOfferingsFactory {

    /**
     * Parses [offeringsJson] (the body of a GET offerings response: `offerings`, `current_offering_id`,
     * optionally `ui_config`) into [Offerings].
     *
     * Product lookup order for each package:
     * 1. [productOverridesByStoreId], keyed by the package's `platform_product_identifier`.
     * 2. A fabricated default [TestStoreProduct] for known [PackageType]s (same defaults used by the
     *    internal paywall previews).
     * 3. A generic fabricated monthly product for custom package identifiers.
     */
    public fun createOfferings(
        offeringsJson: JSONObject,
        productOverridesByStoreId: Map<String, TestStoreProduct> = emptyMap(),
    ): Offerings =
        FixtureOfferingParser(productOverridesByStoreId)
            .createOfferings(offeringsJson, productsById = emptyMap())
}

private class FixtureOfferingParser(
    private val productOverridesByStoreId: Map<String, TestStoreProduct>,
) : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct {
        val storeProductId = packageJson.optString("platform_product_identifier")
        productOverridesByStoreId[storeProductId]?.let { return it }

        val packageIdentifier = packageJson.getString("identifier")
        val packageType = PackageType.values().firstOrNull { it.identifier == packageIdentifier }

        return packageType?.let { defaultTestStoreProduct(it) }
            ?: genericTestStoreProduct(storeProductId.ifEmpty { packageIdentifier })
    }

    private fun genericTestStoreProduct(productId: String): TestStoreProduct =
        TestStoreProduct(
            id = productId,
            name = productId,
            title = "$productId (App name)",
            price = Price(amountMicros = 9_990_000, currencyCode = "USD", formatted = "$ 9.99"),
            description = productId,
            period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )
}
