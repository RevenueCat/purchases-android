package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import java.util.Locale

class DiscountVariableProcessingTests {

    private companion object {
        private const val OFFERING_ID = "offering_identifier"

        private fun usd(amountMicros: Long, formatted: String) =
            Price(amountMicros = amountMicros, currencyCode = "USD", formatted = formatted)

        private fun subscriptionPackage(
            id: String,
            packageType: PackageType,
            price: Price,
            period: Period?,
        ) = Package(
            identifier = id,
            packageType = packageType,
            product = TestStoreProduct(
                id = "com.revenuecat.$id",
                name = id,
                title = "$id (App name)",
                description = id,
                price = price,
                period = period,
            ),
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = OFFERING_ID),
        )

        private val monthlyPackage = subscriptionPackage(
            id = "monthly",
            packageType = PackageType.MONTHLY,
            price = usd(10_000_000, "$10.00"),
            period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )

        private val annualPackage = subscriptionPackage(
            id = "annual",
            packageType = PackageType.ANNUAL,
            price = usd(60_000_000, "$60.00"),
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )

        private val quarterlyPackage = subscriptionPackage(
            id = "quarterly",
            packageType = PackageType.THREE_MONTH,
            price = usd(27_000_000, "$27.00"),
            period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
        )

        private val lifetimePackage = subscriptionPackage(
            id = "lifetime",
            packageType = PackageType.LIFETIME,
            price = usd(119_000_000, "$119.00"),
            period = null,
        )
    }

    // region product.absolute_discount

    @Test
    fun `absolute discount is expressed over the package's own period`() {
        // A $10.00/mo anchor against a $60.00 annual package: the saving over the year
        // being bought is ($10.00 x 12) - $60.00 = $60.00, not the $5.00 per-month gap.
        val result = processTemplate(
            template = "{{ product.absolute_discount }}",
            rcPackage = annualPackage,
            mostExpensivePricePerMonthMicros = 10_000_000,
        )

        assertThat(result).isEqualTo("$60.00")
    }

    @Test
    fun `absolute discount is empty for the anchor package itself`() {
        val result = processTemplate(
            template = "{{ product.absolute_discount }}",
            rcPackage = monthlyPackage,
            mostExpensivePricePerMonthMicros = 10_000_000,
        )

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `absolute discount is empty without an anchor`() {
        val result = processTemplate(
            template = "{{ product.absolute_discount }}",
            rcPackage = annualPackage,
            mostExpensivePricePerMonthMicros = null,
        )

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `absolute discount is empty for a package with no period`() {
        // Lifetime has no period to normalize the anchor onto.
        val result = processTemplate(
            template = "{{ product.absolute_discount }}",
            rcPackage = lifetimePackage,
            mostExpensivePricePerMonthMicros = 10_000_000,
        )

        assertThat(result).isEqualTo("")
    }

    // endregion

    // region product.offer_relative_discount / product.offer_absolute_discount

    @Test
    fun `offer discounts compare the offer price to the standard price`() {
        // $10.00/mo base with a $6.00/mo intro: saves $4.00, 40% off.
        val option = offerOption(
            offerPrice = usd(6_000_000, "$6.00"),
            offerPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )

        assertThat(
            processTemplate(
                template = "{{ product.offer_relative_discount }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("40%")

        assertThat(
            processTemplate(
                template = "{{ product.offer_absolute_discount }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("$4.00")
    }

    @Test
    fun `offer discounts are empty when the offer period differs from the base period`() {
        // A 2-week offer on a monthly product doesn't cover the same span as the base price.
        val option = offerOption(
            offerPrice = usd(6_000_000, "$6.00"),
            offerPeriod = Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W"),
        )

        assertThat(
            processTemplate(
                template = "{{ product.offer_relative_discount }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("")

        assertThat(
            processTemplate(
                template = "{{ product.offer_absolute_discount }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("")
    }

    @Test
    fun `offer discounts are empty for a free offer`() {
        // A free month would otherwise read as "100%" off / a full month's saving.
        val option = offerOption(
            offerPrice = usd(0, "Free"),
            offerPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )

        assertThat(
            processTemplate(
                template = "{{ product.offer_relative_discount }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("")
    }

    @Test
    fun `offer discounts ignore iso8601 formatting differences between equal periods`() {
        // Period's generated equals() also covers `iso8601`, so comparing whole Periods
        // would blank a legitimate offer whose duration string is spelled differently.
        val option = offerOption(
            offerPrice = usd(15_000_000, "$15.00"),
            offerPeriod = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P0Y3M0D"),
        )

        assertThat(
            processTemplate(
                template = "{{ product.offer_relative_discount }}",
                rcPackage = quarterlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("44%")

        assertThat(
            processTemplate(
                template = "{{ product.offer_absolute_discount }}",
                rcPackage = quarterlyPackage,
                subscriptionOption = option,
            ),
        ).isEqualTo("$12.00")
    }

    // endregion

    // region product.relative_discount_with_offer / product.absolute_discount_with_offer

    @Test
    fun `with_offer variables price the offer against the anchor`() {
        // A $9.99/mo anchor vs. a $99.99/yr annual carrying a paid intro year at $52.99,
        // i.e. $4.42/mo. The base-price comparison would report a much smaller number.
        val annual = subscriptionPackage(
            id = "annual",
            packageType = PackageType.ANNUAL,
            price = usd(99_990_000, "$99.99"),
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )
        val option = offerOption(
            offerPrice = usd(52_990_000, "$52.99"),
            offerPeriod = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            cycles = 1,
        )

        assertThat(
            processTemplate(
                template = "{{ product.relative_discount_with_offer }}",
                rcPackage = annual,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 9_990_000,
            ),
        ).isEqualTo("56%")

        assertThat(
            processTemplate(
                template = "{{ product.absolute_discount_with_offer }}",
                rcPackage = annual,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 9_990_000,
            ),
        ).isEqualTo("$66.89")
    }

    @Test
    fun `absolute_discount_with_offer spans every offer cycle`() {
        // 3 cycles at $6.00/mo against a $10.00/mo anchor: $4.00 x 3 = $12.00.
        val option = offerOption(
            offerPrice = usd(6_000_000, "$6.00"),
            offerPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            cycles = 3,
        )

        assertThat(
            processTemplate(
                template = "{{ product.absolute_discount_with_offer }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo("$12.00")
    }

    @Test
    fun `with_offer variables fall back to the base comparison without an offer`() {
        assertThat(
            processTemplate(
                template = "{{ product.absolute_discount_with_offer }}",
                rcPackage = annualPackage,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo(
            processTemplate(
                template = "{{ product.absolute_discount }}",
                rcPackage = annualPackage,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        )
    }

    @Test
    fun `a perpetual discount keeps the relative variant but empties the absolute one`() {
        // Play sends billingCycleCount 0 (not null) for an infinite phase, so the recurrence
        // mode is what tells us there is no term over which a total saving accrues. The
        // monthly rate is still known, so the relative variant keeps working.
        val option = offerOption(
            offerPrice = usd(6_000_000, "$6.00"),
            offerPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            cycles = 0,
            mode = RecurrenceMode.INFINITE_RECURRING,
        )

        assertThat(
            processTemplate(
                template = "{{ product.relative_discount_with_offer }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo("40%")

        assertThat(
            processTemplate(
                template = "{{ product.absolute_discount_with_offer }}",
                rcPackage = monthlyPackage,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo("")
    }

    @Test
    fun `a free offer is treated as no offer rather than 100 percent off`() {
        val option = offerOption(
            offerPrice = usd(0, "Free"),
            offerPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            cycles = 1,
        )

        assertThat(
            processTemplate(
                template = "{{ product.relative_discount_with_offer }}",
                rcPackage = annualPackage,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo(
            processTemplate(
                template = "{{ product.relative_discount_with_offer }}",
                rcPackage = annualPackage,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        )
    }

    @Test
    fun `a single-payment offer is billed exactly once`() {
        // Regression: Play's getBillingCycleCount() is a primitive int, so a NON_RECURRING
        // phase arrives as 0 rather than null. Treating that as "no cycles" multiplied the
        // offer total out to zero and silently blanked a fully determinable saving.
        val annual = subscriptionPackage(
            id = "annual_single_payment",
            packageType = PackageType.ANNUAL,
            price = usd(99_990_000, "$99.99"),
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )
        val option = offerOption(
            offerPrice = usd(52_990_000, "$52.99"),
            offerPeriod = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            cycles = 0,
            mode = RecurrenceMode.NON_RECURRING,
        )

        assertThat(
            processTemplate(
                template = "{{ product.absolute_discount_with_offer }}",
                rcPackage = annual,
                subscriptionOption = option,
                mostExpensivePricePerMonthMicros = 9_990_000,
            ),
        ).isEqualTo("$66.89")
    }

    @Test
    fun `with_offer variables are empty for a package with no period`() {
        assertThat(
            processTemplate(
                template = "{{ product.absolute_discount_with_offer }}",
                rcPackage = lifetimePackage,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo("")

        assertThat(
            processTemplate(
                template = "{{ product.relative_discount_with_offer }}",
                rcPackage = lifetimePackage,
                mostExpensivePricePerMonthMicros = 10_000_000,
            ),
        ).isEqualTo("")
    }

    // endregion

    private fun offerOption(
        offerPrice: Price,
        offerPeriod: Period,
        cycles: Int? = 3,
        mode: RecurrenceMode = RecurrenceMode.FINITE_RECURRING,
    ): SubscriptionOption {
        val phase = mockk<PricingPhase> {
            every { price } returns offerPrice
            every { billingPeriod } returns offerPeriod
            every { billingCycleCount } returns cycles
            every { recurrenceMode } returns mode
        }

        return mockk {
            every { freePhase } returns null
            every { introPhase } returns phase
        }
    }

    private fun processTemplate(
        template: String,
        rcPackage: Package,
        subscriptionOption: SubscriptionOption? = null,
        mostExpensivePricePerMonthMicros: Long? = null,
    ): String {
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
                mostExpensivePricePerMonthMicros = mostExpensivePricePerMonthMicros,
            ),
            rcPackage = rcPackage,
            subscriptionOption = subscriptionOption,
            currencyLocale = Locale.US,
            dateLocale = Locale.US,
            date = Date(),
        )
    }
}
