package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Tests that verify custom variables are correctly replaced in text templates.
 *
 * Custom variables support two syntaxes:
 * - `{{ custom.key }}` - Standard syntax
 * - `{{ $custom.key }}` - Alternative syntax with dollar sign
 *
 * Resolution priority:
 * 1. SDK-provided values (customVariables)
 * 2. Dashboard defaults (defaultCustomVariables)
 * 3. Empty string (with warning log)
 */
class CustomVariableProcessingTests {

    private companion object {
        private const val OFFERING_ID = "offering_identifier"

        private val productYearlyUsd = TestStoreProduct(
            id = "com.revenuecat.annual_product",
            name = "Annual",
            title = "Annual (App name)",
            description = "Annual",
            price = Price(
                amountMicros = 2_000_000,
                currencyCode = "USD",
                formatted = "$ 2.00",
            ),
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            freeTrialPricingPhase = null,
            introPricePricingPhase = null,
        )

        private val packageYearlyUsd = Package(
            identifier = "package_yearly",
            packageType = PackageType.ANNUAL,
            product = productYearlyUsd,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = OFFERING_ID),
        )
    }

    // region Basic custom variable replacement

    @Test
    fun `custom variable with custom prefix is replaced`() {
        val template = "Hello, {{ custom.name }}!"
        val customVariables = mapOf("name" to "John")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, John!")
    }

    @Test
    fun `custom variable with dollar custom prefix is replaced`() {
        val template = "Hello, {{ \$custom.name }}!"
        val customVariables = mapOf("name" to "Jane")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, Jane!")
    }

    @Test
    fun `multiple custom variables are replaced`() {
        val template = "{{ custom.greeting }}, {{ custom.name }}! You have {{ custom.points }} points."
        val customVariables = mapOf(
            "greeting" to "Welcome",
            "name" to "Alice",
            "points" to "100",
        )

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Welcome, Alice! You have 100 points.")
    }

    @Test
    fun `mixed custom and dollar custom prefixes work together`() {
        val template = "{{ custom.first }} and {{ \$custom.second }}"
        val customVariables = mapOf(
            "first" to "One",
            "second" to "Two",
        )

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("One and Two")
    }

    // endregion

    // region Resolution priority

    @Test
    fun `SDK-provided value takes priority over dashboard default`() {
        val template = "Hello, {{ custom.name }}!"
        val customVariables = mapOf("name" to "SDK Value")
        val defaultCustomVariables = mapOf("name" to "Dashboard Default")

        val result = processTemplate(
            template,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        assertThat(result).isEqualTo("Hello, SDK Value!")
    }

    @Test
    fun `dashboard default is used when SDK value is not provided`() {
        val template = "Hello, {{ custom.name }}!"
        val customVariables = emptyMap<String, String>()
        val defaultCustomVariables = mapOf("name" to "Dashboard Default")

        val result = processTemplate(
            template,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        assertThat(result).isEqualTo("Hello, Dashboard Default!")
    }

    @Test
    fun `empty string is returned when variable is not found anywhere`() {
        val template = "Hello, {{ custom.unknown }}!"
        val customVariables = emptyMap<String, String>()
        val defaultCustomVariables = emptyMap<String, String>()

        val result = processTemplate(
            template,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        assertThat(result).isEqualTo("Hello, !")
    }

    @Test
    fun `partial resolution works correctly`() {
        val template = "{{ custom.found }} and {{ custom.missing }}"
        val customVariables = mapOf("found" to "Present")
        val defaultCustomVariables = emptyMap<String, String>()

        val result = processTemplate(
            template,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        assertThat(result).isEqualTo("Present and ")
    }

    // endregion

    // region Functions with custom variables

    @Test
    fun `uppercase function works with custom variables`() {
        val template = "{{ custom.name | uppercase }}"
        val customVariables = mapOf("name" to "john")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("JOHN")
    }

    @Test
    fun `lowercase function works with custom variables`() {
        val template = "{{ custom.name | lowercase }}"
        val customVariables = mapOf("name" to "JANE")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("jane")
    }

    @Test
    fun `capitalize function works with custom variables`() {
        val template = "{{ custom.name | capitalize }}"
        val customVariables = mapOf("name" to "alice")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Alice")
    }

    @Test
    fun `function works with dollar custom prefix`() {
        val template = "{{ \$custom.name | uppercase }}"
        val customVariables = mapOf("name" to "bob")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("BOB")
    }

    @Test
    fun `function is not applied when variable is missing`() {
        val template = "{{ custom.missing | uppercase }}"
        val customVariables = emptyMap<String, String>()

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("")
    }

    // endregion

    // region Mixed with product variables

    @Test
    fun `custom variables work alongside product variables`() {
        val template = "{{ custom.greeting }}, get {{ product.store_product_name }} now!"
        val customVariables = mapOf("greeting" to "Hello")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, get Annual now!")
    }

    @Test
    fun `template with only product variables is not affected by custom variables`() {
        val template = "{{ product.price }}"
        val customVariables = mapOf("price" to "Should not appear")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("$ 2.00")
    }

    // endregion

    // region Edge cases

    @Test
    fun `empty custom variable value is used correctly`() {
        val template = "Value: [{{ custom.empty }}]"
        val customVariables = mapOf("empty" to "")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Value: []")
    }

    @Test
    fun `custom variable with special characters in value`() {
        val template = "{{ custom.special }}"
        val customVariables = mapOf("special" to "Hello, World! @#\$%^&*()")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, World! @#\$%^&*()")
    }

    @Test
    fun `custom variable key with underscores`() {
        val template = "{{ custom.user_first_name }}"
        val customVariables = mapOf("user_first_name" to "John")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("John")
    }

    @Test
    fun `custom variable key with numbers`() {
        val template = "{{ custom.discount2024 }}"
        val customVariables = mapOf("discount2024" to "50%")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("50%")
    }

    @Test
    fun `template without any variables returns unchanged`() {
        val template = "This has no variables"
        val customVariables = mapOf("unused" to "value")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("This has no variables")
    }

    @Test
    fun `custom variable with multiline value`() {
        val template = "{{ custom.multiline }}"
        val customVariables = mapOf("multiline" to "Line 1\nLine 2")

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Line 1\nLine 2")
    }

    // endregion

    private fun processTemplate(
        template: String,
        customVariables: Map<String, String> = emptyMap(),
        defaultCustomVariables: Map<String, String> = emptyMap(),
    ): String {
        val variableDataProvider = VariableDataProvider(MockResourceProvider())
        return VariableProcessorV2.processVariables(
            template = template,
            localizedVariableKeys = variableLocalizationKeysForEnUs(),
            variableConfig = UiConfig.VariableConfig(),
            variableDataProvider = variableDataProvider,
            packageContext = PackageContext(
                discountRelativeToMostExpensivePerMonth = null,
                showZeroDecimalPlacePrices = false,
            ),
            rcPackage = packageYearlyUsd,
            currencyLocale = Locale.US,
            dateLocale = Locale.US,
            date = Date(),
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )
    }
}
