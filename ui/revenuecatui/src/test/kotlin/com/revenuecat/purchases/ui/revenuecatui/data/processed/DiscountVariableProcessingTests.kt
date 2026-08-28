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

    private fun offerOption(offerPrice: Price, offerPeriod: Period): SubscriptionOption {
        val phase = mockk<PricingPhase> {
            every { price } returns offerPrice
            every { billingPeriod } returns offerPeriod
            every { billingCycleCount } returns 3
            every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
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
