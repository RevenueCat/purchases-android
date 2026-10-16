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
class DiscountVariableProcessingTests {

    private companion object {
        private const val MOST_EXPENSIVE_MONTHLY_MICROS = 9_990_000L

        private val year = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y")
        private val threeMonths = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M")
        private val month = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
        private val week = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W")

        private val oneWeekFreeTrial = phase(week, cycles = 1, micros = 0)

        private val offerVariables = listOf(
            "product.offer_relative_discount",
            "product.offer_absolute_discount",
            "product.offer_relative_discount_vs_standard",
            "product.offer_absolute_discount_vs_standard",
        )

        private fun usd(micros: Long) = Price(formatted = "", amountMicros = micros, currencyCode = "USD")

        private fun phase(period: Period, cycles: Int, micros: Long) = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = cycles,
            price = usd(micros),
        )

        private fun subscription(
            priceMicros: Long,
            period: Period,
            packageType: PackageType,
            freeTrial: PricingPhase? = null,
            intro: PricingPhase? = null,
        ) = Package(
            identifier = packageType.identifier!!,
            packageType = packageType,
            product = TestStoreProduct(
                id = "com.revenuecat.discount_variables",
                name = "Pro",
                title = "Pro",
                description = "Pro",
                price = usd(priceMicros),
                period = period,
                freeTrialPricingPhase = freeTrial,
                introPricePricingPhase = intro,
            ),
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "offering"),
        )

        private val yearlyWithOneWeekTrial = subscription(
            priceMicros = 59_990_000,
            period = year,
            packageType = PackageType.ANNUAL,
            freeTrial = oneWeekFreeTrial,
        )

        private fun monthlyWithThreeHalfPriceMonths() = subscription(
            priceMicros = 9_990_000,
            period = month,
            packageType = PackageType.MONTHLY,
            intro = phase(month, cycles = 3, micros = 4_990_000),
        )
    }

    @Test
    fun `offer discounts vs standard prorate a free trial over the first year`() {
        // 59.99 - 59.99 * 358 / 365 = 1.15
        assertThat(process("product.offer_absolute_discount_vs_standard", yearlyWithOneWeekTrial)).isEqualTo("$1.15")
        assertThat(process("product.offer_relative_discount_vs_standard", yearlyWithOneWeekTrial)).isEqualTo("2%")
    }

    @Test
    fun `offer discounts vs most expensive include the offer in the first year`() {
        // 9.99 * 12 - 59.99 * 358 / 365 = 61.04
        assertThat(process("product.offer_absolute_discount", yearlyWithOneWeekTrial, MOST_EXPENSIVE_MONTHLY_MICROS))
            .isEqualTo("$61.04")
        assertThat(process("product.offer_relative_discount", yearlyWithOneWeekTrial, MOST_EXPENSIVE_MONTHLY_MICROS))
            .isEqualTo("51%")
    }

    @Test
    fun `absolute discount compares the standard price with the most expensive over the product period`() {
        // 9.99 * 12 - 59.99, ignoring the offer
        assertThat(process("product.absolute_discount", yearlyWithOneWeekTrial, MOST_EXPENSIVE_MONTHLY_MICROS))
            .isEqualTo("$59.89")
    }

    @Test
    fun `offer discounts count only the first period of a recurring offer`() {
        val pkg = monthlyWithThreeHalfPriceMonths()

        assertThat(process("product.offer_absolute_discount_vs_standard", pkg)).isEqualTo("$5.00")
        assertThat(process("product.offer_relative_discount_vs_standard", pkg)).isEqualTo("50%")
    }

    @Test
    fun `offer discounts prorate a single payment offer longer than the period`() {
        val pkg = subscription(
            9_990_000,
            month,
            PackageType.MONTHLY,
            intro = phase(threeMonths, cycles = 1, micros = 19_990_000),
        )

        // 9.99 - 19.99 / 3
        assertThat(process("product.offer_absolute_discount_vs_standard", pkg)).isEqualTo("$3.33")
    }

    @Test
    fun `offer discounts charge the regular price for the period after a short offer`() {
        val pkg = subscription(9_990_000, month, PackageType.MONTHLY, freeTrial = oneWeekFreeTrial)

        // 9.99 * 7 / 30
        assertThat(process("product.offer_absolute_discount_vs_standard", pkg)).isEqualTo("$2.33")
    }

    @Test
    fun `offer discounts count a free trial and then an intro price`() {
        val pkg = subscription(
            9_990_000,
            month,
            PackageType.MONTHLY,
            freeTrial = oneWeekFreeTrial,
            intro = phase(month, cycles = 1, micros = 4_990_000),
        )

        // 9.99 - 4.99 * 23 / 30
        assertThat(process("product.offer_absolute_discount_vs_standard", pkg)).isEqualTo("$6.16")
        assertThat(process("product.offer_relative_discount_vs_standard", pkg)).isEqualTo("62%")
    }

    @Test
    fun `absolute discounts drop trailing zeros when configured`() {
        val pkg = monthlyWithThreeHalfPriceMonths()

        assertThat(process("product.offer_absolute_discount_vs_standard", pkg, showZeroDecimalPlacePrices = true))
            .isEqualTo("$5")
    }

    @Test
    fun `offer discounts are empty without an offer`() {
        val pkg = subscription(59_990_000, year, PackageType.ANNUAL)

        offerVariables.forEach { variable ->
            assertThat(process(variable, pkg, MOST_EXPENSIVE_MONTHLY_MICROS)).describedAs(variable).isEmpty()
        }
        assertThat(process("product.absolute_discount", pkg, MOST_EXPENSIVE_MONTHLY_MICROS)).isEqualTo("$59.89")
    }

    @Test
    fun `discounts vs most expensive are empty without a most expensive price`() {
        assertThat(process("product.offer_relative_discount", yearlyWithOneWeekTrial)).isEmpty()
        assertThat(process("product.offer_absolute_discount", yearlyWithOneWeekTrial)).isEmpty()
        assertThat(process("product.absolute_discount", yearlyWithOneWeekTrial)).isEmpty()
    }

    @Test
    fun `absolute discount is empty for the most expensive product`() {
        val pkg = subscription(MOST_EXPENSIVE_MONTHLY_MICROS, month, PackageType.MONTHLY)

        assertThat(process("product.absolute_discount", pkg, MOST_EXPENSIVE_MONTHLY_MICROS)).isEmpty()
    }

    @Test
    fun `discount variables are empty for lifetime`() {
        val lifetime = Package(
            identifier = PackageType.LIFETIME.identifier!!,
            packageType = PackageType.LIFETIME,
            product = TestStoreProduct(
                id = "com.revenuecat.lifetime",
                name = "Lifetime",
                title = "Lifetime",
                description = "Lifetime",
                price = usd(119_490_000),
            ),
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "offering"),
        )

        (offerVariables + "product.absolute_discount").forEach { variable ->
            assertThat(process(variable, lifetime, MOST_EXPENSIVE_MONTHLY_MICROS)).describedAs(variable).isEmpty()
        }
    }

    private fun process(
        variable: String,
        rcPackage: Package,
        mostExpensivePricePerMonthMicros: Long? = null,
        showZeroDecimalPlacePrices: Boolean = false,
    ): String = VariableProcessorV2.processVariables(
        template = "{{ $variable }}",
        localizedVariableKeys = variableLocalizationKeysForEnUs(),
        variableConfig = UiConfig.VariableConfig(
            variableCompatibilityMap = emptyMap(),
            functionCompatibilityMap = emptyMap(),
        ),
        variableDataProvider = VariableDataProvider(MockResourceProvider()),
        packageContext = PackageContext(
            discountRelativeToMostExpensivePerMonth = null,
            showZeroDecimalPlacePrices = showZeroDecimalPlacePrices,
            mostExpensivePricePerMonthMicros = mostExpensivePricePerMonthMicros,
        ),
        rcPackage = rcPackage,
        currencyLocale = Locale.US,
        dateLocale = Locale.US,
        date = Date(),
    )
}
