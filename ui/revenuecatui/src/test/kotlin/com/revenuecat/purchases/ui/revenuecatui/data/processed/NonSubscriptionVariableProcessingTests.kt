package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Price variables render the plain price for products with no billing period, regardless of the
 * package identifier. Mirrors the behaviour of `VariableHandlerV2` on iOS.
 */
@Suppress("DEPRECATION")
class NonSubscriptionVariableProcessingTests {

    private val customLifetimePackage = nonSubscriptionPackage(
        packageType = PackageType.CUSTOM,
        identifier = "custom_lifetime_tier_1",
    )

    private val predefinedLifetimePackage = nonSubscriptionPackage(
        packageType = PackageType.LIFETIME,
        identifier = PackageType.LIFETIME.identifier!!,
    )

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.w(any()) } returns Unit
        every { Logger.e(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `price per period abbreviated renders the price for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_period_abbreviated }}", customLifetimePackage))
            .isEqualTo("$49.99")
    }

    @Test
    fun `price per period renders the price for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_period }}", customLifetimePackage))
            .isEqualTo("$49.99")
    }

    @Test
    fun `price per period abbreviated renders the price for the predefined lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_period_abbreviated }}", predefinedLifetimePackage))
            .isEqualTo("$49.99")
    }

    @Test
    fun `price per day renders the full price for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_day }}", customLifetimePackage)).isEqualTo("$49.99")
    }

    @Test
    fun `price per week renders the full price for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_week }}", customLifetimePackage)).isEqualTo("$49.99")
    }

    @Test
    fun `price per month renders the full price for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_month }}", customLifetimePackage)).isEqualTo("$49.99")
    }

    @Test
    fun `price per year renders the full price for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.price_per_year }}", customLifetimePackage)).isEqualTo("$49.99")
    }

    @Test
    fun `period is empty for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.period }}", customLifetimePackage)).isEmpty()
    }

    @Test
    fun `period abbreviated is empty for a custom-identifier lifetime package`() {
        assertThat(processTemplate("{{ product.period_abbreviated }}", customLifetimePackage)).isEmpty()
    }

    private fun nonSubscriptionPackage(packageType: PackageType, identifier: String) =
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

    private fun processTemplate(template: String, rcPackage: Package): String {
        return VariableProcessorV2.processVariables(
            template = template,
            localizedVariableKeys = variableLocalizationKeysForEnUs(),
            variableConfig = UiConfig.VariableConfig(
                variableCompatibilityMap = emptyMap(),
                functionCompatibilityMap = emptyMap(),
            ),
            variableDataProvider = VariableDataProvider(MockResourceProvider()),
            packageContext = PackageContext(
                discountRelativeToMostExpensivePerMonth = null,
                showZeroDecimalPlacePrices = false,
            ),
            rcPackage = rcPackage,
            subscriptionOption = null,
            currencyLocale = Locale.US,
            dateLocale = Locale.US,
            date = Date(),
        )
    }
}
