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

/**
 * Tests to verify that currency locale is properly applied to offer price variables
 * in VariableProcessorV2, fixing the mixed currencies bug (PW-133).
 *
 * The key fix is that `product.offer_price` now uses the currencyLocale parameter
 * instead of the pre-formatted Price.formatted value.
 */
class OfferPriceLocaleTests {

    private companion object {
        private const val OFFERING_ID = "offering_identifier"

        // Price pre-formatted with a locale that puts currency after the number (German style)
        private val introPrice = Price(
            amountMicros = 1_000_000,
            currencyCode = "USD",
            formatted = "1,00 $", // German-style formatting (wrong for US)
        )

        private val fullPrice = Price(
            amountMicros = 10_000_000,
            currencyCode = "USD",
            formatted = "10,00 $", // German-style formatting (wrong for US)
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
    fun `product_offer_price uses currencyLocale not pre-formatted price`() {
        val template = "{{ product.offer_price }}"

        // Process with US locale - should use US formatting regardless of pre-formatted value
        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = null,
            currencyLocale = Locale.US,
        )

        // Should be US formatted ($ before number) not the pre-formatted German style ($ after number)
        assertThat(result).startsWith("$")
        assertThat(result).contains("1")
    }

    @Test
    fun `product_offer_price uses German locale when specified`() {
        val template = "{{ product.offer_price }}"

        val result = processTemplate(
            template = template,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = null,
            currencyLocale = Locale.GERMANY,
        )

        // Should be German formatted ($ after number)
        assertThat(result).endsWith("$")
        assertThat(result).contains("1")
    }

    @Test
    fun `secondary_offer_price uses currencyLocale`() {
        val freePricingPhase = mockk<PricingPhase> {
            every { price } returns Price(amountMicros = 0, currencyCode = "USD", formatted = "Free")
            every { billingPeriod } returns Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W")
            every { billingCycleCount } returns 1
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
        }

        val introPricingPhase = mockk<PricingPhase> {
            every { price } returns Price(
                amountMicros = 500_000,
                currencyCode = "USD",
                formatted = "0,50 $", // German formatting
            )
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
            currencyLocale = Locale.US,
        )

        // Should be US formatted ($ before number)
        assertThat(result).startsWith("$")
        assertThat(result).contains("0.50")
    }

    @Test
    fun `currency_symbol and offer_price use same locale`() {
        val offerPriceTemplate = "{{ product.offer_price }}"
        val symbolTemplate = "{{ product.currency_symbol }}"

        val offerPrice = processTemplate(
            template = offerPriceTemplate,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = null,
            currencyLocale = Locale.US,
        )

        val symbol = processTemplate(
            template = symbolTemplate,
            rcPackage = packageWithIntroOffer,
            subscriptionOption = null,
            currencyLocale = Locale.US,
        )

        // Both should use the same US locale, so offer_price should start with the currency symbol
        assertThat(offerPrice).startsWith(symbol)
    }

    private fun processTemplate(
        template: String,
        rcPackage: Package,
        subscriptionOption: SubscriptionOption?,
        currencyLocale: Locale,
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
            currencyLocale = currencyLocale,
            dateLocale = Locale.US,
            date = Date(),
        )
    }
}
