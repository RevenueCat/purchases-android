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

class OfferVariableProcessingTests {

    private companion object {
        private const val OFFERING_ID = "offering_identifier"

        private val introPrice = Price(
            amountMicros = 1_000_000,
            currencyCode = "USD",
            formatted = "$1.00",
        )

        private val promoPrice = Price(
            amountMicros = 500_000,
            currencyCode = "USD",
            formatted = "$0.50",
        )

        private val fullPrice = Price(
            amountMicros = 10_000_000,
            currencyCode = "USD",
            formatted = "$10.00",
        )

        private val productWithIntroOffer = TestStoreProduct(
            id = "com.revenuecat.monthly_product",
            name = "Monthly",
            title = "Monthly (App name)",
            description = "Monthly",
            price = fullPrice,
            period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            freeTrialPeriod = null,
            introPrice = introPrice,
        )

        private val packageWithIntroOffer = Package(
            identifier = "package_monthly",
            packageType = PackageType.MONTHLY,
            product = productWithIntroOffer,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = OFFERING_ID),
        )

        private val productWithNoIntroOffer = TestStoreProduct(
            id = "com.revenuecat.monthly_product_no_intro",
            name = "Monthly",
            title = "Monthly (App name)",
            description = "Monthly",
            price = fullPrice,
            period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )

        private val packageWithNoIntroOffer = Package(
            identifier = "package_monthly_no_intro",
            packageType = PackageType.MONTHLY,
            product = productWithNoIntroOffer,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = OFFERING_ID),
        )

        private val optionWithNoDiscountPhases = mockk<SubscriptionOption> {
            every { freePhase } returns null
            every { introPhase } returns null
        }
    }

    @Test
    fun `offer variables use package default option when no subscription option provided`() {
        val template = "{{ product.offer_price }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = null,
        )

        assertThat(result).isEqualTo("$1.00")
    }

    @Test
    fun `offer variables use subscription option when provided`() {
        val promoPricingPhase = mockk<PricingPhase> {
            every { price } returns promoPrice
            every { billingPeriod } returns Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
            every { billingCycleCount } returns 1
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
        }

        val promoOption = mockk<SubscriptionOption> {
            every { freePhase } returns null
            every { introPhase } returns promoPricingPhase
        }

        val template = "{{ product.offer_price }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = promoOption,
        )

        assertThat(result).isEqualTo("$0.50")
    }

    @Test
    fun `offer period uses subscription option when provided`() {
        val promoPricingPhase = mockk<PricingPhase> {
            every { price } returns promoPrice
            every { billingPeriod } returns Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W")
            every { billingCycleCount } returns 1
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
        }

        val promoOption = mockk<SubscriptionOption> {
            every { freePhase } returns null
            every { introPhase } returns promoPricingPhase
        }

        val template = "{{ product.offer_period }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = promoOption,
        )

        assertThat(result).isEqualTo("week")
    }

    @Test
    fun `offer period with unit uses subscription option when provided`() {
        val promoPricingPhase = mockk<PricingPhase> {
            every { price } returns promoPrice
            every { billingPeriod } returns Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W")
            every { billingCycleCount } returns 1
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
        }

        val promoOption = mockk<SubscriptionOption> {
            every { freePhase } returns null
            every { introPhase } returns promoPricingPhase
        }

        val template = "{{ product.offer_period_with_unit }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = promoOption,
        )

        assertThat(result).isEqualTo("2 weeks")
    }

    // region Fallback tests - offer variables fall back to product values when no discount phase

    @Test
    fun `offer price falls back to product price when subscription option has no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_price }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("$10.00")
    }

    @Test
    fun `offer price falls back to product price when no subscription option and no intro offer`() {
        val result = processTemplate(
            template = "{{ product.offer_price }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = null,
        )

        assertThat(result).isEqualTo("$10.00")
    }

    @Test
    fun `offer period falls back to product period when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("month")
    }

    @Test
    fun `offer period abbreviated falls back to product period abbreviated when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period_abbreviated }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("mo")
    }

    @Test
    fun `offer period with unit falls back to product period with unit when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period_with_unit }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("1 month")
    }

    @Test
    fun `offer period in days falls back to product period in days when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period_in_days }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        // 1 month ≈ 30 days
        assertThat(result).isEqualTo("30")
    }

    @Test
    fun `offer period in weeks falls back to product period in weeks when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period_in_weeks }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        // 1 month ≈ 4 weeks
        assertThat(result).isEqualTo("4")
    }

    @Test
    fun `offer period in months falls back to product period in months when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period_in_months }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("1")
    }

    @Test
    fun `offer period in years falls back to product period in years when no discount phases`() {
        val result = processTemplate(
            template = "{{ product.offer_period_in_years }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        // 1 month ≈ 0.08 years → rounds to 0
        assertThat(result).isEqualTo("0")
    }

    @Test
    fun `offer variables still use intro offer from package when subscription option has discount phases`() {
        val template = "{{ product.offer_price }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = null,
        )

        // Should use the intro price, not fall back
        assertThat(result).isEqualTo("$1.00")
    }

    // endregion

    @Test
    fun `secondary offer uses subscription option intro phase when free phase exists`() {
        val freePricingPhase = mockk<PricingPhase> {
            every { price } returns Price(amountMicros = 0, currencyCode = "USD", formatted = "Free")
            every { billingPeriod } returns Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W")
            every { billingCycleCount } returns 1
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
        }

        val introPricingPhase = mockk<PricingPhase> {
            every { price } returns promoPrice
            every { billingPeriod } returns Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
            every { billingCycleCount } returns 1
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
        }

        val promoOption = mockk<SubscriptionOption> {
            every { freePhase } returns freePricingPhase
            every { introPhase } returns introPricingPhase
        }

        val template = "{{ product.secondary_offer_price }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = promoOption,
        )

        assertThat(result).isEqualTo("$0.50")
    }

    // region Secondary offer fallback tests

    @Test
    fun `secondary offer price falls back to product price when no secondary discount phase`() {
        // optionWithNoDiscountPhases has no freePhase, so secondaryDiscountPhase returns null
        val result = processTemplate(
            template = "{{ product.secondary_offer_price }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("$10.00")
    }

    @Test
    fun `secondary offer period falls back to product period when no secondary discount phase`() {
        val result = processTemplate(
            template = "{{ product.secondary_offer_period }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("month")
    }

    @Test
    fun `secondary offer period abbreviated falls back to product period abbreviated when no secondary discount phase`() {
        val result = processTemplate(
            template = "{{ product.secondary_offer_period_abbreviated }}",
            rcPackage = packageWithNoIntroOffer,
            subscriptionOption = optionWithNoDiscountPhases,
        )

        assertThat(result).isEqualTo("mo")
    }

    // endregion

    private fun processTemplate(
        template: String,
        rcPackage: Package,
        subscriptionOption: SubscriptionOption?,
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
            rcPackage = rcPackage,
            subscriptionOption = subscriptionOption,
            currencyLocale = Locale.US,
            dateLocale = Locale.US,
            date = Date(),
        )
    }
}
