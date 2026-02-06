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
