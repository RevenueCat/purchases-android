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
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.PackageComponent
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
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessorV2.Variable
import com.revenuecat.purchases.ui.revenuecatui.extensions.copy
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.variableLocalizationKeysForEsMx
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.net.URL
import java.util.Date

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class TextComponentViewVariablesTests(
    private val variable: String,
    args: Args,
    private val expected: String,
) {

    class Args(
        val packages: List<Package>,
        val locale: String,
        val storefrontCountryCode: String,
        val variableLocalizations: NonEmptyMap<String, NonEmptyMap<VariableLocalizationKey, String>>,
        val date: Date = Date(),
        val variableConfig: VariableConfig = VariableConfig(),
    ) {
        constructor(
             packages: List<Package>,
             locale: String,
             storefrontCountryCode: String,
             variableLocalizations: NonEmptyMap<VariableLocalizationKey, String>,
             variablesLocale: String = locale,
             date: Date = Date(),
             variableConfig: VariableConfig = VariableConfig(),
        ): this(
            packages = packages,
            locale = locale,
            storefrontCountryCode = storefrontCountryCode,
            variableLocalizations = nonEmptyMapOf(variablesLocale to variableLocalizations),
            date = date,
            variableConfig = variableConfig,
        )
    }

    @Suppress("LargeClass")
    companion object {
        private const val MILLIS_2025_01_25 = 1737763200000
        private const val STORE_COUNTRY_WITHOUT_DECIMALS = "MX"
        private val zeroDecimalPlaceCountries = listOf(STORE_COUNTRY_WITHOUT_DECIMALS)

        private const val OFFERING_ID = "offering_identifier"
        private val productYearlyUsdTwoOffers = TestStoreProduct(
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
            freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
            // TestStoreProduct will add a monthly intro with this price.
            introPrice = Price(
                amountMicros = 1_000_000,
                currencyCode = "USD",
                formatted = "$ 1.00",
            )
        )
        private val productMonthlyUsdOneOffer = TestStoreProduct(
            id = "com.revenuecat.monthly_product",
            name = "Monthly",
            title = "Monthly (App name)",
            price = Price(
                amountMicros = 1_000_000,
                currencyCode = "USD",
                formatted = "$ 1.00",
            ),
            description = "Monthly",
            period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )
        private val productQuarterlyUsdNoOffers = TestStoreProduct(
            id = "com.revenuecat.quarterly_product",
            name = "3 month",
            title = "3 month (App name)",
            price = Price(
                amountMicros = 23_990_000,
                currencyCode = "USD",
                formatted = "$23.99"
            ),
            description = "3 month",
            period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
        )
        private val productYearlyMxnOneOffer = TestStoreProduct(
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
        private val productYearlyJpyOneOffer = TestStoreProduct(
            id = "com.revenuecat.annual_product",
            name = "Annual",
            title = "Annual (App name)",
            price = Price(
                amountMicros = 20000_000_000,
                currencyCode = "JPY",
                formatted = "¥20,000",
            ),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )
        private val productYearlySekOneOffer = TestStoreProduct(
            id = "com.revenuecat.annual_product",
            name = "Annual",
            title = "Annual (App name)",
            price = Price(
                amountMicros = 20000_000_000,
                currencyCode = "SEK",
                formatted = "20.000,00 kr",
            ),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )
        private val productLifetimeUsd = TestStoreProduct(
            id = "com.revenuecat.lifetime_product",
            name = "Lifetime",
            title = "Lifetime (App name)",
            price = Price(
                amountMicros = 200_000_000,
                currencyCode = "USD",
                formatted = "$ 200.00",
            ),
            description = "Lifetime",
            period = null,
        )
        private val productOneTimePurchaseUsd = TestStoreProduct(
            id = "com.revenuecat.otp_product",
            name = "OneTimePurchase",
            title = "OneTimePurchase (App name)",
            price = Price(
                amountMicros = 200_000_000,
                currencyCode = "USD",
                formatted = "$ 200.00",
            ),
            description = "OneTimePurchase",
            period = null,
        )
        private val productWithUpperCaseName = productYearlyUsdTwoOffers.copy(name = "ANNUAL")
        private val productWithLowerCaseName = productYearlyUsdTwoOffers.copy(name = "annual")

        @Suppress("DEPRECATION")
        private val packageYearlyUsdTwoOffers = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_yearly",
            offering = OFFERING_ID,
            product = productYearlyUsdTwoOffers,
        )

        @Suppress("DEPRECATION")
        private val packageMonthlyUsdOneOffer = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_monthly",
            offering = OFFERING_ID,
            product = productMonthlyUsdOneOffer,
        )

        @Suppress("DEPRECATION")
        private val packageQuarterlyUsdNoOffers = Package(
            packageType = PackageType.THREE_MONTH,
            identifier = PackageType.THREE_MONTH.identifier!!,
            offering = OFFERING_ID,
            product = productQuarterlyUsdNoOffers,
        )

        @Suppress("DEPRECATION")
        private val packageYearlyMxnOneOffer = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_yearly",
            offering = OFFERING_ID,
            product = productYearlyMxnOneOffer,
        )

        @Suppress("DEPRECATION")
        private val packageYearlyJpyOneOffer = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_yearly",
            offering = OFFERING_ID,
            product = productYearlyJpyOneOffer,
        )

        @Suppress("DEPRECIATION")
        private val packageYearlySekOneOffer = Package(
            packageType = PackageType.ANNUAL,
            identifier = "package_yearly",
            offering = OFFERING_ID,
            product = productYearlySekOneOffer,
        )

        @Suppress("DEPRECATION")
        private val packageLifetimeUsd = Package(
            packageType = PackageType.LIFETIME,
            identifier = PackageType.LIFETIME.identifier!!,
            offering = OFFERING_ID,
            product = productLifetimeUsd,
        )
        @Suppress("DEPRECATION")
        private val packageOneTimePurchaseUsd = Package(
            packageType = PackageType.CUSTOM,
            identifier = "package_otp",
            offering = OFFERING_ID,
            product = productOneTimePurchaseUsd,
        )

        private val packageWithUpperCaseName = packageYearlyUsdTwoOffers.copy(product = productWithUpperCaseName)
        private val packageWithLowerCaseName = packageYearlyUsdTwoOffers.copy(product = productWithLowerCaseName)

        @Suppress("LongMethod", "Unused", "CyclomaticComplexMethod")
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} = {2}")
        fun parameters(): Collection<*> = Variable.values().map { variableName ->
            when (variableName) {
                Variable.PRODUCT_CURRENCY_CODE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "USD"
                )

                Variable.PRODUCT_CURRENCY_SYMBOL -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$"
                )

                Variable.PRODUCT_PERIODLY -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "yearly"
                )

                Variable.PRODUCT_PRICE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyMxnOneOffer),
                        locale = "es_$STORE_COUNTRY_WITHOUT_DECIMALS",
                        storefrontCountryCode = STORE_COUNTRY_WITHOUT_DECIMALS,
                        variableLocalizations = variableLocalizationKeysForEsMx(),
                    ),
                    "\$200"
                )

                Variable.PRODUCT_PRICE_PER_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$ 2.00/year"
                )

                Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$ 2.00/yr"
                )

                Variable.PRODUCT_PRICE_PER_DAY -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$0.00"
                )

                Variable.PRODUCT_PRICE_PER_WEEK -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$0.03"
                )

                Variable.PRODUCT_PRICE_PER_MONTH -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$0.16"
                )

                Variable.PRODUCT_PRICE_PER_YEAR -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$2.00"
                )

                Variable.PRODUCT_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "year"
                )

                Variable.PRODUCT_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "yr"
                )

                Variable.PRODUCT_PERIOD_IN_DAYS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "365"
                )

                Variable.PRODUCT_PERIOD_IN_WEEKS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "52"
                )

                Variable.PRODUCT_PERIOD_IN_MONTHS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "12"
                )

                Variable.PRODUCT_PERIOD_IN_YEARS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1"
                )

                Variable.PRODUCT_PERIOD_WITH_UNIT -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1 year"
                )

                Variable.PRODUCT_OFFER_PRICE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "free"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_DAY -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "free"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_WEEK -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "free"
                )

                Variable.PRODUCT_OFFER_PRICE_PER_MONTH -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    ""
                )

                Variable.PRODUCT_OFFER_PRICE_PER_YEAR -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    ""
                )

                Variable.PRODUCT_OFFER_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "week"
                )

                Variable.PRODUCT_OFFER_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "wk"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_DAYS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "7"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_WEEKS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1"
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_MONTHS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    ""
                )

                Variable.PRODUCT_OFFER_PERIOD_IN_YEARS -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    ""
                )

                Variable.PRODUCT_OFFER_PERIOD_WITH_UNIT -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "1 week"
                )

                Variable.PRODUCT_OFFER_END_DATE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                        date = Date(MILLIS_2025_01_25),
                    ),
                    "February 1, 2025"
                )

                Variable.PRODUCT_SECONDARY_OFFER_PRICE -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "\$1.00"
                )

                Variable.PRODUCT_SECONDARY_OFFER_PERIOD -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "month"
                )

                Variable.PRODUCT_SECONDARY_OFFER_PERIOD_ABBREVIATED -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "mo"
                )

                Variable.PRODUCT_RELATIVE_DISCOUNT -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers, packageMonthlyUsdOneOffer),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "83%"
                )

                Variable.PRODUCT_STORE_PRODUCT_NAME -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    "Annual"
                )

                Variable.COUNT_DAYS_WITH_ZERO,
                Variable.COUNT_DAYS_WITHOUT_ZERO,
                Variable.COUNT_HOURS_WITH_ZERO,
                Variable.COUNT_HOURS_WITHOUT_ZERO,
                Variable.COUNT_MINUTES_WITH_ZERO,
                Variable.COUNT_MINUTES_WITHOUT_ZERO,
                Variable.COUNT_SECONDS_WITH_ZERO,
                Variable.COUNT_SECONDS_WITHOUT_ZERO,
                -> arrayOf(
                    "{{ ${variableName.identifier} }}",
                    Args(
                        packages = listOf(packageYearlyUsdTwoOffers),
                        locale = "en_US",
                        storefrontCountryCode = "US",
                        variableLocalizations = variableLocalizationKeysForEnUs(),
                    ),
                    ""
                )
            }
        } + listOf(
            // Lifetime:
            arrayOf(
                "{{ ${Variable.PRODUCT_CURRENCY_CODE.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "USD"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_CURRENCY_SYMBOL.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIODLY.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "lifetime"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00",
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_DAY.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_WEEK.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_MONTH.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_YEAR.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "lifetime"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "lifetime"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_DAYS.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_WEEKS.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_MONTHS.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_YEARS.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_WITH_UNIT.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "lifetime"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_RELATIVE_DISCOUNT.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd, packageMonthlyUsdOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_STORE_PRODUCT_NAME.identifier} }}",
                Args(
                    packages = listOf(packageLifetimeUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Lifetime"
            )
        ) + listOf(
            // One-time purchase:
            arrayOf(
                "{{ ${Variable.PRODUCT_CURRENCY_CODE.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "USD"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_CURRENCY_SYMBOL.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIODLY.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "\$ 200.00"
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_DAY.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_WEEK.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_MONTH.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_YEAR.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_DAYS.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_WEEKS.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_MONTHS.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_IN_YEARS.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_WITH_UNIT.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_RELATIVE_DISCOUNT.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd, packageMonthlyUsdOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                ""
            ), arrayOf(
                "{{ ${Variable.PRODUCT_STORE_PRODUCT_NAME.identifier} }}",
                Args(
                    packages = listOf(packageOneTimePurchaseUsd),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "OneTimePurchase"
            )
        ) + listOf(
            // Foreign currency:
            arrayOf(
                "{{ ${Variable.PRODUCT_CURRENCY_SYMBOL.identifier} }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "es_ES",
                    storefrontCountryCode = "ES",
                    variableLocalizations = variableLocalizationKeysForEsMx(),
                ),
                "US\$"
            ),
            // Spurious digits:
            // JPY is not part of STORE_COUNTRY_WITHOUT_DECIMALS, because the stores already correctly format JPY prices
            // without decimals. However, when we perform calculations on such a price, we should be careful not to add
            // spurious decimals.
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_DAY.identifier} }}",
                Args(
                    packages = listOf(packageYearlyJpyOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "JP",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "￥54"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_WEEK.identifier} }}",
                Args(
                    packages = listOf(packageYearlyJpyOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "JP",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "￥383"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_MONTH.identifier} }}",
                Args(
                    packages = listOf(packageYearlyJpyOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "JP",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "￥1,666"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_YEAR.identifier} }}",
                Args(
                    packages = listOf(packageYearlyJpyOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "JP",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "￥20,000"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageYearlyJpyOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "JP",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "¥20,000/year"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageYearlyJpyOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "JP",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "¥20,000/yr"
            ),
            // storefrontCountryCode different from device locale. We should always prefer the storefront country when
            // formatting calculated prices, to avoid discrepancies with prices coming from the store directly.
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_DAY.identifier} }}",
                Args(
                    packages = listOf(packageYearlySekOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "SE",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "54,79 kr"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_WEEK.identifier} }}",
                Args(
                    packages = listOf(packageYearlySekOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "SE",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "383,56 kr"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_MONTH.identifier} }}",
                Args(
                    packages = listOf(packageYearlySekOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "SE",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "1 666,66 kr"
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_YEAR.identifier} }}",
                Args(
                    packages = listOf(packageYearlySekOneOffer),
                    locale = "en_US",
                    storefrontCountryCode = "SE",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "20 000,00 kr"
            ),
            // Functions:
            arrayOf(
                "{{ product.store_product_name | lowercase }}",
                Args(
                    packages = listOf(packageWithUpperCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "annual"
            ),
            arrayOf(
                "{{ product.store_product_name | capitalize }}",
                Args(
                    packages = listOf(packageWithUpperCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                // Capitalize doesn't do anything if the first character is already upper case.
                "ANNUAL"
            ),
            arrayOf(
                "{{ product.store_product_name | uppercase }}",
                Args(
                    packages = listOf(packageWithLowerCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "ANNUAL"
            ),
            arrayOf(
                "{{ product.store_product_name | capitalize }}",
                Args(
                    packages = listOf(packageWithLowerCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Annual"
            ),
            arrayOf(
                "{{ product.store_product_name | lowercase | capitalize }}",
                Args(
                    packages = listOf(packageWithUpperCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Annual"
            ),
            // Multiple variables, with prefix and suffix text.
            arrayOf(
                "Products: {{ product.store_product_name | lowercase | capitalize }} and " +
                    "{{ product.store_product_name | lowercase }}.",
                Args(
                    packages = listOf(packageWithUpperCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Products: Annual and annual."
            ),
            // Defensive:
            arrayOf(
                "{{product.store_product_name}}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Annual"
            ),
            arrayOf(
                "{{       product.store_product_name    }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Annual"
            ),
            arrayOf(
                "{{product.store_product_name|lowercase|capitalize}}",
                Args(
                    packages = listOf(packageWithUpperCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Annual"
            ),
            arrayOf(
                "{{       product.store_product_name|lowercase       |    capitalize       }}",
                Args(
                    packages = listOf(packageWithUpperCaseName),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "Annual"
            ),
            // Fallbacks:
            arrayOf(
                "{{ name_variable_from_the_year_5202 }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                    variableConfig = VariableConfig(
                        variableCompatibilityMap = mapOf(
                            "name_variable_from_the_year_5202" to "product.store_product_name"
                        )
                    )
                ),
                "Annual"
            ),
            arrayOf(
                "{{ name_variable_from_the_year_5202 | uppercase_function_from_the_year_5202 }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                    variableConfig = VariableConfig(
                        variableCompatibilityMap = mapOf(
                            "name_variable_from_the_year_5202" to "product.store_product_name"
                        ),
                        functionCompatibilityMap = mapOf(
                            "uppercase_function_from_the_year_5202" to "uppercase"
                        )
                    )
                ),
                "ANNUAL"
            ),
            arrayOf(
                "{{ name_variable_from_the_year_5202 | uppercase_function_from_the_year_5202 }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                    variableConfig = VariableConfig(
                        // Empty on purpose
                        variableCompatibilityMap = emptyMap(),
                        functionCompatibilityMap = mapOf(
                            "uppercase_function_from_the_year_5202" to "uppercase"
                        )
                    )
                ),
                ""
            ),
            arrayOf(
                "{{ name_variable_from_the_year_5202 | uppercase_function_from_the_year_5202 }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                    variableConfig = VariableConfig(
                        variableCompatibilityMap = mapOf(
                            "name_variable_from_the_year_5202" to "product.store_product_name"
                        ),
                        // Empty on purpose
                        functionCompatibilityMap = emptyMap(),
                    )
                ),
                // Unknown function, so the value is unprocessed.
                "Annual"
            ),
            arrayOf(
                "{{ name_variable_from_the_year_5202 | uppercase_function_from_the_year_5202 }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                    variableConfig = VariableConfig(
                        // The backwards compatible values are also unknown.
                        variableCompatibilityMap = mapOf(
                            "name_variable_from_the_year_5202" to "name_variable_from_the_year_3000"
                        ),
                        functionCompatibilityMap = mapOf(
                            "uppercase_function_from_the_year_5202" to "uppercase_function_from_the_year_3000"
                        )
                    )
                ),
                ""
            ),
            arrayOf(
                "{{ name_variable_from_the_year_5202 | uppercase_function_from_the_year_5202 }}",
                Args(
                    packages = listOf(packageYearlyUsdTwoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                    variableConfig = VariableConfig(
                        // Recursion, just in case.
                        variableCompatibilityMap = mapOf(
                            "name_variable_from_the_year_5202" to "name_variable_from_the_year_3000",
                            "name_variable_from_the_year_3000" to "product.store_product_name",
                        ),
                        functionCompatibilityMap = mapOf(
                            "uppercase_function_from_the_year_5202" to "uppercase_function_from_the_year_3000",
                            "uppercase_function_from_the_year_3000" to "uppercase",
                        )
                    )
                ),
                "ANNUAL"
            ),
            // Products with a period spanning multiple months:
            arrayOf(
                "{{ ${Variable.PRODUCT_PERIODLY.identifier} }}",
                Args(
                    packages = listOf(packageQuarterlyUsdNoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "3 months",
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageQuarterlyUsdNoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "$23.99/3 months",
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageQuarterlyUsdNoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "$23.99/3mo",
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD.identifier} }}",
                Args(
                    packages = listOf(packageQuarterlyUsdNoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "3 months",
            ),
            arrayOf(
                "{{ ${Variable.PRODUCT_PERIOD_ABBREVIATED.identifier} }}",
                Args(
                    packages = listOf(packageQuarterlyUsdNoOffers),
                    locale = "en_US",
                    storefrontCountryCode = "US",
                    variableLocalizations = variableLocalizationKeysForEnUs(),
                ),
                "3mo",
            ),
            // LocaleId doesn't exactly match
            arrayOf(
                "{{ ${Variable.PRODUCT_PERIODLY.identifier} }}",
                Args(
                    packages = listOf(packageYearlyMxnOneOffer),
                    locale = "es_MX",
                    storefrontCountryCode = "MX",
                    variableLocalizations = nonEmptyMapOf(
                        "en_US" to variableLocalizationKeysForEnUs(),
                        // Our variables are keyed by just "es" but the rest of the paywall is keyed by "es_MX".
                        "es" to variableLocalizationKeysForEsMx()
                    )
                ),
                "anualmente",
            ),
        )
    }

    private val packages = args.packages
    private val locale = args.locale
    private val storefrontCountryCode = args.storefrontCountryCode
    private val variableLocalizations = args.variableLocalizations.mapKeys { LocaleId(it.key) }
    private val date = args.date
    private val variableConfig = args.variableConfig

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Should correctly process variables`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedText = "Here's a variable in parentheses: ($expected)."
        val textWithVariable = LocalizationData.Text("Here's a variable in parentheses: ($variable).")

        val defaultLocaleIdentifier = LocaleId(locale)
        val textKey = LocalizationKey("key_selected")
        val localizations = nonEmptyMapOf(defaultLocaleIdentifier to nonEmptyMapOf(textKey to textWithVariable))
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val textComponent = TextComponent(text = textKey, color = textColor)
        val packageComponents = packages.map { pkg ->
            PackageComponent(
                packageId = pkg.identifier,
                isSelectedByDefault = false,
                stack = StackComponent(components = emptyList())
            )
        }
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(textComponent) + packageComponents),
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
            availablePackages = packages,
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(
                    localizations = variableLocalizations,
                    variableConfig = variableConfig,
                ),
                data = data
            ),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = storefrontCountryCode,
            dateProvider = { date },
        )

        val styleFactory = StyleFactory(
            localizations = localizations,
            variableLocalizations = variableLocalizations,
            offering = offering,
        )
        val style = styleFactory.create(textComponent).getOrThrow().componentStyle as TextComponentStyle

        // Act
        setContent { TextComponentView(style = style, state = state, modifier = Modifier.testTag("text")) }

        // Assert
        // Select the first package to make sure the variables reflect its values.
        state.update(packages.first().identifier)

        onNodeWithTag("text")
            .onChild()
            .assertTextEquals(expectedText)
    }
}
