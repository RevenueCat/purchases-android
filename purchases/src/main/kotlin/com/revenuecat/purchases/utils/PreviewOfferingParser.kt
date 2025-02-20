package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct
import org.json.JSONObject

/**
 * This is instantiated via reflection by revenuecatui, in `TemplatePreviews.kt`, to be able to show previews of entire
 * v2 Paywall templates.
 */
@Suppress("UnusedPrivateClass", "unused", "LongMethod")
@OptIn(InternalRevenueCatAPI::class)
private class PreviewOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        // Ignoring productsById and just returning a product based on the identifier in the packageJson.
        val identifier = packageJson.getString("identifier")
        val packageType = PackageType.values().first { packageType -> packageType.identifier == identifier }

        // These products are the same as those in TestData in revenuecatui.
        return when (packageType) {
            PackageType.LIFETIME -> TestStoreProduct(
                id = "com.revenuecat.lifetime_product",
                name = "Lifetime",
                title = "Lifetime (App name)",
                price = Price(amountMicros = 1_000_000_000, currencyCode = "USD", formatted = "$1,000"),
                description = "Lifetime",
                period = null,
            )

            PackageType.ANNUAL -> TestStoreProduct(
                id = "com.revenuecat.annual_product",
                name = "Annual",
                title = "Annual (App name)",
                price = Price(amountMicros = 67_990_000, currencyCode = "USD", formatted = "$67.99"),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
                freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            )

            PackageType.SIX_MONTH -> TestStoreProduct(
                id = "com.revenuecat.semester_product",
                name = "6 month",
                title = "6 month (App name)",
                price = Price(amountMicros = 39_990_000, currencyCode = "USD", formatted = "$39.99"),
                description = "6 month",
                period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
            )

            PackageType.THREE_MONTH -> TestStoreProduct(
                id = "com.revenuecat.quarterly_product",
                name = "3 month",
                title = "3 month (App name)",
                price = Price(amountMicros = 23_990_000, currencyCode = "USD", formatted = "$23.99"),
                description = "3 month",
                period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
                freeTrialPeriod = Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W"),
                introPrice = Price(amountMicros = 3_990_000, currencyCode = "USD", formatted = "$3.99"),
            )

            PackageType.TWO_MONTH -> TestStoreProduct(
                id = "com.revenuecat.bimonthly_product",
                name = "2 month",
                title = "2 month (App name)",
                price = Price(amountMicros = 15_990_000, currencyCode = "USD", formatted = "$15.99"),
                description = "2 month",
                period = Period(value = 2, unit = Period.Unit.MONTH, iso8601 = "P2M"),
                introPrice = Price(amountMicros = 3_990_000, currencyCode = "USD", formatted = "$3.99"),
            )

            PackageType.MONTHLY -> TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                name = "Monthly",
                title = "Monthly (App name)",
                price = Price(amountMicros = 7_990_000, currencyCode = "USD", formatted = "$7.99"),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            )

            PackageType.WEEKLY -> TestStoreProduct(
                id = "com.revenuecat.weekly_product",
                name = "Weekly",
                title = "Weekly (App name)",
                price = Price(amountMicros = 1_490_000, currencyCode = "USD", formatted = "$1.49"),
                description = "Weekly",
                period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
            )

            else -> null
        }
    }
}
