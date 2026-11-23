package com.revenuecat.purchases.ui.revenuecatui.data.processed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PeriodCountLocaleTests {

    private companion object {
        private val ARABIC = Locale("ar", "SA")
        private val ARABIC_LATIN_DIGITS = Locale.forLanguageTag("ar-SA-u-nu-latn")

        private val WESTERN_DIGIT = Regex("[0-9]")
        private val ARABIC_INDIC_DIGIT = Regex("[٠-٩]")

        private val threeMonths = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M")
        private val twoWeeks = Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W")

        private val quarterlyWithTwoWeekIntro = Package(
            identifier = PackageType.THREE_MONTH.identifier!!,
            packageType = PackageType.THREE_MONTH,
            product = TestStoreProduct(
                id = "com.revenuecat.quarterly",
                name = "Pro",
                title = "Pro",
                description = "Pro",
                price = Price(formatted = "", amountMicros = 29_990_000, currencyCode = "USD"),
                period = threeMonths,
                introPricePricingPhase = PricingPhase(
                    billingPeriod = twoWeeks,
                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                    billingCycleCount = 1,
                    price = Price(formatted = "", amountMicros = 990_000, currencyCode = "USD"),
                ),
            ),
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "offering"),
        )

        private val periodCountVariables = listOf(
            "product.periodly",
            "product.period",
            "product.period_abbreviated",
            "product.period_with_unit",
            "product.price_per_period",
            "product.price_per_period_abbreviated",
            "product.period_in_days",
            "product.period_in_weeks",
            "product.period_in_months",
            "product.offer_period_in_days",
            "product.offer_period_in_weeks",
            "product.offer_period_with_unit",
        )
    }

    @Test
    fun `period counts use the currency locale digits`() {
        periodCountVariables.forEach { variable ->
            val result = process(variable, ARABIC)

            assertThat(result).describedAs(variable).containsPattern(ARABIC_INDIC_DIGIT.pattern)
            assertThat(result).describedAs(variable).doesNotContainPattern(WESTERN_DIGIT.pattern)
        }
    }

    @Test
    fun `period counts match the price digits`() {
        assertThat(process("product.price_per_period", ARABIC))
            .contains("٣")
            .doesNotContainPattern(WESTERN_DIGIT.pattern)
        assertThat(process("product.period_in_months", ARABIC)).isEqualTo("٣")
        assertThat(process("product.offer_period_with_unit", ARABIC)).isEqualTo("٢ weeks")
    }

    @Test
    fun `period counts ignore the device locale`() {
        val defaultLocale = Locale.getDefault()
        Locale.setDefault(ARABIC)
        try {
            periodCountVariables.forEach { variable ->
                val result = process(variable, Locale.US)

                assertThat(result).describedAs(variable).containsPattern(WESTERN_DIGIT.pattern)
                assertThat(result).describedAs(variable).doesNotContainPattern(ARABIC_INDIC_DIGIT.pattern)
            }
            assertThat(process("product.period", Locale.US)).isEqualTo("3 months")
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun `period counts use western digits for a latin numbering system locale`() {
        periodCountVariables.forEach { variable ->
            val result = process(variable, ARABIC_LATIN_DIGITS)

            assertThat(result).describedAs(variable).containsPattern(WESTERN_DIGIT.pattern)
            assertThat(result).describedAs(variable).doesNotContainPattern(ARABIC_INDIC_DIGIT.pattern)
        }
    }

    private fun process(variable: String, currencyLocale: Locale): String = VariableProcessorV2.processVariables(
        template = "{{ $variable }}",
        localizedVariableKeys = variableLocalizationKeysForEnUs(),
        variableConfig = UiConfig.VariableConfig(
            variableCompatibilityMap = emptyMap(),
            functionCompatibilityMap = emptyMap(),
        ),
        variableDataProvider = VariableDataProvider(MockResourceProvider()),
        packageContext = PackageContext(
            discountRelativeToMostExpensivePerMonth = null,
            showZeroDecimalPlacePrices = false,
            mostExpensivePricePerMonthMicros = null,
        ),
        rcPackage = quarterlyWithTwoWeekIntro,
        currencyLocale = currencyLocale,
        dateLocale = Locale.US,
        date = Date(),
    )
}
