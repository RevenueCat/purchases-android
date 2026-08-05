package com.revenuecat.purchases.utils

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct
import org.json.JSONObject

/**
 * This is instantiated via reflection by revenuecatui, in `TemplatePreviews.kt`, to be able to show previews of entire
 * v2 Paywall templates.
 */
@Suppress("UnusedPrivateClass", "unused", "LongMethod")
private class PreviewOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        // Ignoring productsById and just returning a product based on the identifier in the packageJson.
        val identifier = packageJson.getString("identifier")
        val packageType = PackageType.values().first { packageType -> packageType.identifier == identifier }

        // Prices, periods, and the single introductory offer ($1.99 for 1 week) are kept in
        // sync with the web/dashboard preview variable tables so the paywall rendering-validation
        // screenshots show the same products, prices, and offers across platforms. (This
        // intentionally diverges from TestData in revenuecatui, which is used for other previews.)
        return when (packageType) {
            PackageType.LIFETIME -> TestStoreProduct(
                id = "com.revenuecat.lifetime_product",
                name = "Pro Access",
                title = "Lifetime (App name)",
                price = Price(amountMicros = 119_990_000, currencyCode = "USD", formatted = "$119.99"),
                description = "Lifetime",
                period = null,
            )

            PackageType.ANNUAL -> TestStoreProduct(
                id = "com.revenuecat.annual_product",
                name = "Pro Access",
                title = "Annual (App name)",
                price = Price(amountMicros = 69_990_000, currencyCode = "USD", formatted = "$69.99"),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
                introPricePricingPhase = introductoryOfferPhase(),
            )

            PackageType.SIX_MONTH -> TestStoreProduct(
                id = "com.revenuecat.semester_product",
                name = "Pro Access",
                title = "6 month (App name)",
                price = Price(amountMicros = 39_990_000, currencyCode = "USD", formatted = "$39.99"),
                description = "6 month",
                period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
                introPricePricingPhase = introductoryOfferPhase(),
            )

            PackageType.THREE_MONTH -> TestStoreProduct(
                id = "com.revenuecat.quarterly_product",
                name = "Pro Access",
                title = "3 month (App name)",
                price = Price(amountMicros = 24_990_000, currencyCode = "USD", formatted = "$24.99"),
                description = "3 month",
                period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
                introPricePricingPhase = introductoryOfferPhase(),
            )

            PackageType.TWO_MONTH -> TestStoreProduct(
                id = "com.revenuecat.bimonthly_product",
                name = "Pro Access",
                title = "2 month (App name)",
                price = Price(amountMicros = 17_990_000, currencyCode = "USD", formatted = "$17.99"),
                description = "2 month",
                period = Period(value = 2, unit = Period.Unit.MONTH, iso8601 = "P2M"),
                introPricePricingPhase = introductoryOfferPhase(),
            )

            PackageType.MONTHLY -> TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                name = "Pro Access",
                title = "Monthly (App name)",
                price = Price(amountMicros = 9_990_000, currencyCode = "USD", formatted = "$9.99"),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
                introPricePricingPhase = introductoryOfferPhase(),
            )

            PackageType.WEEKLY -> TestStoreProduct(
                id = "com.revenuecat.weekly_product",
                name = "Pro Access",
                title = "Weekly (App name)",
                price = Price(amountMicros = 2_990_000, currencyCode = "USD", formatted = "$2.99"),
                description = "Weekly",
                period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
                introPricePricingPhase = introductoryOfferPhase(),
            )

            else -> null
        }
    }

    // A single introductory offer of $1.99 for 1 week, attached to every subscription, matching
    // the offer values in the web/dashboard preview tables and the iOS preview harness.
    private fun introductoryOfferPhase(): PricingPhase = PricingPhase(
        billingPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
        recurrenceMode = RecurrenceMode.FINITE_RECURRING,
        billingCycleCount = 1,
        price = Price(amountMicros = 1_990_000, currencyCode = "USD", formatted = "$1.99"),
    )
}
