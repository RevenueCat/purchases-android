package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@Suppress("DEPRECATION")
internal class PackagePeriodExtensionsTest {

    private val localizedVariableKeys: Map<VariableLocalizationKey, String> = mapOf(
        VariableLocalizationKey.LIFETIME to "Lifetime",
        VariableLocalizationKey.MONTHLY to "monthly",
        VariableLocalizationKey.MONTH_SHORT to "mo",
    )

    @Test
    fun `isLifetime is true for the predefined lifetime package`() {
        val pkg = lifetimePackage(
            packageType = PackageType.LIFETIME,
            identifier = PackageType.LIFETIME.identifier!!,
        )

        assertThat(pkg.isLifetime).isTrue()
    }

    @Test
    fun `isLifetime is false for a non-subscription product in a custom-identifier package`() {
        val pkg = lifetimePackage(
            packageType = PackageType.CUSTOM,
            identifier = "custom_lifetime_tier_1",
        )

        assertThat(pkg.isLifetime).isFalse()
    }

    @Test
    fun `isLifetime is false for a subscription product`() {
        assertThat(monthlyPackage().isLifetime).isFalse()
    }

    @Test
    fun `hasNoBillingPeriod is true for a non-subscription product in a custom-identifier package`() {
        val pkg = lifetimePackage(
            packageType = PackageType.CUSTOM,
            identifier = "custom_lifetime_tier_1",
        )

        assertThat(pkg.hasNoBillingPeriod).isTrue()
    }

    @Test
    fun `hasNoBillingPeriod is false for a subscription product`() {
        assertThat(monthlyPackage().hasNoBillingPeriod).isFalse()
    }

    @Test
    fun `productPeriodAbbreviated returns null for a custom-identifier lifetime package`() {
        val pkg = lifetimePackage(
            packageType = PackageType.CUSTOM,
            identifier = "custom_lifetime_tier_1",
        )

        assertThat(pkg.productPeriodAbbreviated(localizedVariableKeys)).isNull()
    }

    @Test
    fun `productPeriodAbbreviated returns the lifetime string for the predefined lifetime package`() {
        val pkg = lifetimePackage(
            packageType = PackageType.LIFETIME,
            identifier = PackageType.LIFETIME.identifier!!,
        )

        assertThat(pkg.productPeriodAbbreviated(localizedVariableKeys)).isEqualTo("Lifetime")
    }

    @Test
    fun `productPeriodAbbreviated returns the abbreviated period for a subscription product`() {
        assertThat(monthlyPackage().productPeriodAbbreviated(localizedVariableKeys)).isEqualTo("mo")
    }

    private fun lifetimePackage(packageType: PackageType, identifier: String) =
        Package(
            packageType = packageType,
            identifier = identifier,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.lifetime_product",
                name = "Lifetime",
                title = "Lifetime (App name)",
                price = Price(amountMicros = 49_990_000, currencyCode = "USD", formatted = "$49.99"),
                description = "Lifetime",
                period = null,
            ),
        )

    private fun monthlyPackage() =
        Package(
            packageType = PackageType.MONTHLY,
            identifier = PackageType.MONTHLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                name = "Monthly",
                title = "Monthly (App name)",
                price = Price(amountMicros = 9_990_000, currencyCode = "USD", formatted = "$9.99"),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )
}
