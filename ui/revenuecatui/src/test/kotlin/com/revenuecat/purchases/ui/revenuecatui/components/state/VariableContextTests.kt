package com.revenuecat.purchases.ui.revenuecatui.components.state

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext.VariableContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class VariableContextTests {

    @Test
    fun `mostExpensivePricePerMonthMicros should be null for empty package list`() {
        // Arrange
        val packages = emptyList<Package>()

        // Act
        val actual = VariableContext(packages).mostExpensivePricePerMonthMicros

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `mostExpensivePricePerMonthMicros should return correct price for single package`() {
        // Arrange
        val package1 = monthlyPackageWithPrice(1_000_000)
        val expected = 1_000_000L

        // Act
        val actual = VariableContext(listOf(package1)).mostExpensivePricePerMonthMicros

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `mostExpensivePricePerMonthMicros should return highest price among multiple monthly packages`() {
        // Arrange
        val package1 = monthlyPackageWithPrice(1_000_000)
        val package2 = monthlyPackageWithPrice(2_000_000)
        val package3 = monthlyPackageWithPrice(500_000)
        val expected = 2_000_000L

        // Act
        val actual = VariableContext(listOf(package1, package2, package3)).mostExpensivePricePerMonthMicros

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `mostExpensivePricePerMonthMicros should return highest price among multiple packages with various periods`() {
        // Arrange
        // Weekly is unusually expensive.
        val weekly = weeklyPackageWithPrice(2_000_000)
        val monthly = monthlyPackageWithPrice(1_000_000)
        val yearly = annualPackageWithPrice(500_000)
        val expected = weekly.product.pricePerMonth()?.amountMicros
        
        // Act
        val actual = VariableContext(listOf(monthly, weekly, yearly)).mostExpensivePricePerMonthMicros

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `mostExpensivePricePerMonthMicros should ignore lifetime packages`() {
        // Arrange
        val package1 = monthlyPackageWithPrice(1_000_000)
        val package2 = lifetimePackageWithPrice(1_000_000_000)
        val expected = 1_000_000L

        // Act
        val actual = VariableContext(listOf(package1, package2)).mostExpensivePricePerMonthMicros

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `mostExpensivePricePerMonthMicros should return null when all packages are lifetime`() {
        // Arrange
        val package1 = lifetimePackageWithPrice(1_000_000_000)
        val package2 = lifetimePackageWithPrice(2_000_000_000)

        // Act
        val actual = VariableContext(listOf(package1, package2)).mostExpensivePricePerMonthMicros

        // Assert
        assertThat(actual).isNull()
    }

    @Suppress("DEPRECATION")
    private fun weeklyPackageWithPrice(amountMicros: Long) =
        Package(
            packageType = PackageType.WEEKLY,
            identifier = PackageType.WEEKLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.weekly_product",
                name = "Weekly",
                title = "Weekly (App name)",
                price = Price(
                    amountMicros = amountMicros,
                    currencyCode = "USD",
                    formatted = formatMicrosToCurrency(amountMicros),
                ),
                description = "Weekly",
                period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
            ),
        )

    @Suppress("DEPRECATION")
    private fun monthlyPackageWithPrice(amountMicros: Long) =
        Package(
            packageType = PackageType.MONTHLY,
            identifier = PackageType.MONTHLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                name = "Monthly",
                title = "Monthly (App name)",
                price = Price(
                    amountMicros = amountMicros,
                    currencyCode = "USD",
                    formatted = formatMicrosToCurrency(amountMicros),
                ),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )

    @Suppress("DEPRECATION")
    private fun annualPackageWithPrice(amountMicros: Long) =
        Package(
            packageType = PackageType.ANNUAL,
            identifier = PackageType.ANNUAL.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.annual_product",
                name = "Annual",
                title = "Annual (App name)",
                price = Price(
                    amountMicros = amountMicros,
                    currencyCode = "USD",
                    formatted = formatMicrosToCurrency(amountMicros),
                ),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
                freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )

    @Suppress("DEPRECATION")
    private fun lifetimePackageWithPrice(amountMicros: Long) =
        Package(
            packageType = PackageType.LIFETIME,
            identifier = PackageType.LIFETIME.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.lifetime_product",
                name = "Lifetime",
                title = "Lifetime (App name)",
                price = Price(
                    amountMicros = amountMicros,
                    currencyCode = "USD",
                    formatted = formatMicrosToCurrency(amountMicros),
                ),
                description = "Lifetime",
                period = null,
            ),
        )

    /**
     * Exact output of this doesn't really matter. Its only use is more helpful errors.
     */
    private fun formatMicrosToCurrency(micros: Long, locale: Locale = Locale.US): String =
        NumberFormat.getCurrencyInstance(locale).format(micros / 1_000_000.0)
}
