package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
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
 *
 * Custom variables accept Map<String, CustomVariableValue> with type-safe values.
 */
@OptIn(InternalRevenueCatAPI::class)
class CustomVariableProcessingTests {

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.w(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

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
        val customVariables = mapOf("name" to CustomVariableValue.String("John"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, John!")
    }

    @Test
    fun `custom variable with dollar custom prefix is replaced`() {
        val template = "Hello, {{ \$custom.name }}!"
        val customVariables = mapOf("name" to CustomVariableValue.String("Jane"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, Jane!")
    }

    @Test
    fun `multiple custom variables are replaced`() {
        val template = "{{ custom.greeting }}, {{ custom.name }}! You have {{ custom.points }} points."
        val customVariables = mapOf(
            "greeting" to CustomVariableValue.String("Welcome"),
            "name" to CustomVariableValue.String("Alice"),
            "points" to CustomVariableValue.Number(100),
        )

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Welcome, Alice! You have 100 points.")
    }

    @Test
    fun `mixed custom and dollar custom prefixes work together`() {
        val template = "{{ custom.first }} and {{ \$custom.second }}"
        val customVariables = mapOf(
            "first" to CustomVariableValue.String("One"),
            "second" to CustomVariableValue.String("Two"),
        )

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("One and Two")
    }

    // endregion

    // region Resolution priority

    @Test
    fun `SDK-provided value takes priority over dashboard default`() {
        val template = "Hello, {{ custom.name }}!"
        val customVariables = mapOf("name" to CustomVariableValue.String("SDK Value"))
        val defaultCustomVariables = mapOf("name" to CustomVariableValue.String("Dashboard Default"))

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
        val customVariables = emptyMap<String, CustomVariableValue>()
        val defaultCustomVariables = mapOf("name" to CustomVariableValue.String("Dashboard Default"))

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
        val customVariables = emptyMap<String, CustomVariableValue>()
        val defaultCustomVariables = emptyMap<String, CustomVariableValue>()

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
        val customVariables = mapOf("found" to CustomVariableValue.String("Present"))
        val defaultCustomVariables = emptyMap<String, CustomVariableValue>()

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
        val customVariables = mapOf("name" to CustomVariableValue.String("john"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("JOHN")
    }

    @Test
    fun `lowercase function works with custom variables`() {
        val template = "{{ custom.name | lowercase }}"
        val customVariables = mapOf("name" to CustomVariableValue.String("JANE"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("jane")
    }

    @Test
    fun `capitalize function works with custom variables`() {
        val template = "{{ custom.name | capitalize }}"
        val customVariables = mapOf("name" to CustomVariableValue.String("alice"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Alice")
    }

    @Test
    fun `function works with dollar custom prefix`() {
        val template = "{{ \$custom.name | uppercase }}"
        val customVariables = mapOf("name" to CustomVariableValue.String("bob"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("BOB")
    }

    @Test
    fun `function is not applied when variable is missing`() {
        val template = "{{ custom.missing | uppercase }}"
        val customVariables = emptyMap<String, CustomVariableValue>()

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("")
    }

    // endregion

    // region Mixed with product variables

    @Test
    fun `custom variables work alongside product variables`() {
        val template = "{{ custom.greeting }}, get {{ product.store_product_name }} now!"
        val customVariables = mapOf("greeting" to CustomVariableValue.String("Hello"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, get Annual now!")
    }

    @Test
    fun `template with only product variables is not affected by custom variables`() {
        val template = "{{ product.price }}"
        val customVariables = mapOf("price" to CustomVariableValue.String("Should not appear"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("$ 2.00")
    }

    // endregion

    // region Edge cases

    @Test
    fun `empty custom variable value is used correctly`() {
        val template = "Value: [{{ custom.empty }}]"
        val customVariables = mapOf("empty" to CustomVariableValue.String(""))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Value: []")
    }

    @Test
    fun `custom variable with special characters in value`() {
        val template = "{{ custom.special }}"
        val customVariables = mapOf("special" to CustomVariableValue.String("Hello, World! @#\$%^&*()"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Hello, World! @#\$%^&*()")
    }

    @Test
    fun `custom variable key with underscores`() {
        val template = "{{ custom.user_first_name }}"
        val customVariables = mapOf("user_first_name" to CustomVariableValue.String("John"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("John")
    }

    @Test
    fun `custom variable key with numbers`() {
        val template = "{{ custom.discount2024 }}"
        val customVariables = mapOf("discount2024" to CustomVariableValue.String("50%"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("50%")
    }

    @Test
    fun `template without any variables returns unchanged`() {
        val template = "This has no variables"
        val customVariables = mapOf("unused" to CustomVariableValue.String("value"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("This has no variables")
    }

    @Test
    fun `custom variable with multiline value`() {
        val template = "{{ custom.multiline }}"
        val customVariables = mapOf("multiline" to CustomVariableValue.String("Line 1\nLine 2"))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Line 1\nLine 2")
    }

    // endregion

    // region Type conversions

    @Test
    fun `integer custom variable is converted to string`() {
        val template = "You have {{ custom.count }} items"
        val customVariables = mapOf("count" to CustomVariableValue.Number(42))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("You have 42 items")
    }

    @Test
    fun `double custom variable is converted to string`() {
        val template = "Price: {{ custom.price }}"
        val customVariables = mapOf("price" to CustomVariableValue.Number(19.99))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Price: 19.99")
    }

    @Test
    fun `boolean true custom variable is converted to string`() {
        val template = "Premium: {{ custom.is_premium }}"
        val customVariables = mapOf("is_premium" to CustomVariableValue.Boolean(true))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Premium: true")
    }

    @Test
    fun `boolean false custom variable is converted to string`() {
        val template = "Premium: {{ custom.is_premium }}"
        val customVariables = mapOf("is_premium" to CustomVariableValue.Boolean(false))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("Premium: false")
    }

    @Test
    fun `long custom variable is converted to string`() {
        val template = "ID: {{ custom.user_id }}"
        val customVariables = mapOf("user_id" to CustomVariableValue.Number(1234567890123L))

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("ID: 1234567890123")
    }

    @Test
    fun `mixed types in same template are converted correctly`() {
        val template = "{{ custom.name }} has {{ custom.points }} points (VIP: {{ custom.is_vip }})"
        val customVariables = mapOf(
            "name" to CustomVariableValue.String("John"),
            "points" to CustomVariableValue.Number(100),
            "is_vip" to CustomVariableValue.Boolean(true),
        )

        val result = processTemplate(template, customVariables = customVariables)

        assertThat(result).isEqualTo("John has 100 points (VIP: true)")
    }

    @Test
    fun `integer custom variable with uppercase function`() {
        val template = "Count: {{ custom.count | uppercase }}"
        val customVariables = mapOf("count" to CustomVariableValue.Number(42))

        val result = processTemplate(template, customVariables = customVariables)

        // Numbers don't change with uppercase, but function is still applied
        assertThat(result).isEqualTo("Count: 42")
    }

    @Test
    fun `non-string default custom variable is converted to string`() {
        val template = "Level: {{ custom.level }}"
        val customVariables = emptyMap<String, CustomVariableValue>()
        val defaultCustomVariables = mapOf("level" to CustomVariableValue.Number(5))

        val result = processTemplate(
            template,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        assertThat(result).isEqualTo("Level: 5")
    }

    @Test
    fun `SDK integer overrides dashboard string default`() {
        val template = "Value: {{ custom.value }}"
        val customVariables = mapOf("value" to CustomVariableValue.Number(999))
        val defaultCustomVariables = mapOf("value" to CustomVariableValue.String("default"))

        val result = processTemplate(
            template,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        assertThat(result).isEqualTo("Value: 999")
    }

    // endregion

    private fun processTemplate(
        template: String,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
        defaultCustomVariables: Map<String, CustomVariableValue> = emptyMap(),
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
