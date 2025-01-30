package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessorV2.Variable
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.helpers.variableLocalizationKeysForEsMx
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.net.URL

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class TextComponentViewVariablesTests(
    private val variable: String,
    args: Args,
    private val expected: String,
) {

    class Args(
        val rcPackage: Package,
        val locale: String,
        val storefrontCountryCode: String,
        val variableLocalizations: NonEmptyMap<VariableLocalizationKey, String>,
    )

    companion object {
        private const val STORE_COUNTRY_WITHOUT_DECIMALS = "MX"
        private val zeroDecimalPlaceCountries = listOf(STORE_COUNTRY_WITHOUT_DECIMALS)

        private const val OFFERING_ID = "offering_identifier"
        private val productYearlyUsd = TestStoreProduct(
            id = "com.revenuecat.annual_product",
            name = "Annual",
            title = "Annual (App name)",
            price = Price(
                amountMicros = 2_000_000,
                currencyCode = "USD",
                formatted = "$ 2.00",
            ),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )
        private val productYearlyMxn = TestStoreProduct(
            id = "com.revenuecat.annual_product",
            name = "Annual",
            title = "Annual (App name)",
            price = Price(
                amountMicros = 200_000_000,
                currencyCode = "MXN",
                formatted = "$ 200",
            ),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )

        @Suppress("DEPRECATION")
        private val packageYearlyUsd = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_yearly",
            offering = OFFERING_ID,
            product = productYearlyUsd,
        )

        @Suppress("DEPRECATION")
        private val packageYearlyMxn = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_yearly",
            offering = OFFERING_ID,
            product = productYearlyMxn,
        )

        @Suppress("LongMethod", "Unused", "CyclomaticComplexMethod")
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} = {2}")
        fun parameters(): Collection<*> = Variable.values().map { variableName ->
            when (variableName) {
                Variable.PRODUCT_CURRENCY_CODE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "USD"
                )

                Variable.PRODUCT_CURRENCY_SYMBOL -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$"
                )

                Variable.PRODUCT_PERIODLY -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "yearly"
                )

                Variable.PRODUCT_PRICE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyMxn,
                        locale = "es_$STORE_COUNTRY_WITHOUT_DECIMALS",
                        storefrontCountryCode = STORE_COUNTRY_WITHOUT_DECIMALS,
                        variableLocalizations = variableLocalizationKeysForEsMx(),
                    ),
                    "\$200"
                )

                Variable.PRODUCT_PRICE_PER_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$ 2.00/year"
                )

                Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$ 2.00/yr"
                )

                Variable.PRODUCT_PRICE_PER_DAY -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$0.01"
                )

                Variable.PRODUCT_PRICE_PER_WEEK -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$0.04"
                )

                Variable.PRODUCT_PRICE_PER_MONTH -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$0.17"
                )

                Variable.PRODUCT_PRICE_PER_YEAR -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$2.00"
                )

                Variable.PRODUCT_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "year"
                )

                Variable.PRODUCT_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "yr"
                )

                Variable.PRODUCT_PERIOD_IN_DAYS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "365"
                )

                Variable.PRODUCT_PERIOD_IN_WEEKS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "52"
                )

                Variable.PRODUCT_PERIOD_IN_MONTHS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "12"
                )

                Variable.PRODUCT_PERIOD_IN_YEARS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1"
                )

                Variable.PRODUCT_PERIOD_WITH_UNIT -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1 year"
                )

                Variable.PRODUCT_OFFER_PRICE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "free"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_DAY -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_WEEK -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_MONTH -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_YEAR -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_OFFER_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "month"
                )

                Variable.PRODUCT_OFFER_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "mo"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_DAYS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "30"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_WEEKS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "4"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_MONTHS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_YEARS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    ""
                )

                Variable.PRODUCT_OFFER_PERIOD_WITH_UNIT -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1 month"
                )

                Variable.PRODUCT_OFFER_END_DATE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_SECONDARY_OFFER_PRICE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_SECONDARY_OFFER_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_SECONDARY_OFFER_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_RELATIVE_DISCOUNT -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.PRODUCT_STORE_PRODUCT_NAME -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        rcPackage = packageYearlyUsd,
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )
            }
        } + listOf(
            arrayOf(
                "{{ ${Variable.PRODUCT_CURRENCY_SYMBOL.identifier} }}",
                Args(
                    rcPackage = packageYearlyUsd,
                    locale = "es_ES",
                    storefrontCountryCode = "ES",
                    variableLocalizations = variableLocalizationKeysForEsMx(),
                ),
                "US\$"
            )
        )
    }

    private val rcPackage = args.rcPackage
    private val locale = args.locale
    private val storefrontCountryCode = args.storefrontCountryCode
    private val variableLocalizations = args.variableLocalizations

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Should correctly process variable values`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedText = "Here's a variable in parentheses: ($expected)."
        val textWithVariable = LocalizationData.Text("Here's a variable in parentheses: ($variable).")

        val defaultLocaleIdentifier = LocaleId(locale)
        val textKey = LocalizationKey("key_selected")
        val localizations = nonEmptyMapOf(defaultLocaleIdentifier to nonEmptyMapOf(textKey to textWithVariable))
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val textComponent = TextComponent(text = textKey, color = textColor)
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(textComponent)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        )
        
        val offering = Offering(
            identifier = OFFERING_ID,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(rcPackage),
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(localizations = mapOf(LocaleId(locale) to variableLocalizations)),
                data = data
            ),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated, storefrontCountryCode = storefrontCountryCode)

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val style = styleFactory.create(textComponent).getOrThrow() as TextComponentStyle

        // Act
        setContent { TextComponentView(style = style, state = state, modifier = Modifier.testTag("text")) }

        // Assert
        // Select the package to make sure the variables reflect its values.
        state.update(selectedPackage = rcPackage)

        onNodeWithTag("text")
            .onChild()
            .assertTextEquals(expectedText)
    }





}
