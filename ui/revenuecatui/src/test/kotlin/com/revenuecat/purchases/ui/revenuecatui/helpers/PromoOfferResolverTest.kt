package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.paywalls.components.common.PromoOfferConfig
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PromoOfferResolverTest {

    @Test
    fun `resolve returns NoConfiguration when offerConfig is null`() {
        val rcPackage = createPackageWithSubscriptionOptions()

        val result = PromoOfferResolver.resolve(rcPackage, null)

        assertThat(result).isInstanceOf(ResolvedOffer.NoConfiguration::class.java)
        assertThat(result.isPromoOffer).isFalse()
    }

    @Test
    fun `resolve returns NoConfiguration when subscriptionOptions is null`() {
        val rcPackage = createPackageWithoutSubscriptionOptions()
        val offerConfig = PromoOfferConfig(offerId = "promo-offer")

        val result = PromoOfferResolver.resolve(rcPackage, offerConfig)

        assertThat(result).isInstanceOf(ResolvedOffer.NoConfiguration::class.java)
        assertThat(result.isPromoOffer).isFalse()
    }

    @Test
    fun `resolve returns ConfiguredOffer when offerId matches`() {
        val matchingOption = createGoogleSubscriptionOption("promo-offer")
        val rcPackage = createPackageWithSubscriptionOptions(listOf(matchingOption))
        val offerConfig = PromoOfferConfig(offerId = "promo-offer")

        val result = PromoOfferResolver.resolve(rcPackage, offerConfig)

        assertThat(result).isInstanceOf(ResolvedOffer.ConfiguredOffer::class.java)
        assertThat(result.subscriptionOption).isEqualTo(matchingOption)
        assertThat(result.isPromoOffer).isTrue()
    }

    @Test
    fun `resolve returns ConfigurationError when offerId does not match any option`() {
        val option = createGoogleSubscriptionOption("other-offer")
        val rcPackage = createPackageWithSubscriptionOptions(listOf(option))
        val offerConfig = PromoOfferConfig(offerId = "non-existent-offer")

        val result = PromoOfferResolver.resolve(rcPackage, offerConfig)

        assertThat(result).isInstanceOf(ResolvedOffer.ConfigurationError::class.java)
        val error = result as ResolvedOffer.ConfigurationError
        assertThat(error.configuredOfferId).isEqualTo("non-existent-offer")
        assertThat(result.isPromoOffer).isFalse()
    }

    @Test
    fun `resolve finds matching option among multiple options`() {
        val option1 = createGoogleSubscriptionOption("offer-1")
        val option2 = createGoogleSubscriptionOption("offer-2")
        val option3 = createGoogleSubscriptionOption("offer-3")
        val rcPackage = createPackageWithSubscriptionOptions(listOf(option1, option2, option3))
        val offerConfig = PromoOfferConfig(offerId = "offer-2")

        val result = PromoOfferResolver.resolve(rcPackage, offerConfig)

        assertThat(result).isInstanceOf(ResolvedOffer.ConfiguredOffer::class.java)
        assertThat(result.subscriptionOption).isEqualTo(option2)
    }

    @Test
    fun `resolve ignores non-GoogleSubscriptionOption options`() {
        val nonGoogleOption = mockk<SubscriptionOption> {
            every { id } returns "some-id"
        }
        val googleOption = createGoogleSubscriptionOption("google-offer")
        val rcPackage = createPackageWithSubscriptionOptions(listOf(nonGoogleOption, googleOption))
        val offerConfig = PromoOfferConfig(offerId = "google-offer")

        val result = PromoOfferResolver.resolve(rcPackage, offerConfig)

        assertThat(result).isInstanceOf(ResolvedOffer.ConfiguredOffer::class.java)
        assertThat(result.subscriptionOption).isEqualTo(googleOption)
    }

    @Test
    fun `ConfigurationError fallbackOption uses defaultOption`() {
        val defaultOption = createGoogleSubscriptionOption("default")
        val rcPackage = createPackageWithSubscriptionOptions(
            options = listOf(defaultOption),
            defaultOption = defaultOption,
        )
        val offerConfig = PromoOfferConfig(offerId = "non-existent")

        val result = PromoOfferResolver.resolve(rcPackage, offerConfig)

        assertThat(result).isInstanceOf(ResolvedOffer.ConfigurationError::class.java)
        assertThat(result.subscriptionOption).isEqualTo(defaultOption)
    }

    private fun createPackageWithSubscriptionOptions(
        options: List<SubscriptionOption> = emptyList(),
        defaultOption: SubscriptionOption? = options.firstOrNull(),
    ): Package {
        val subscriptionOptions = mockk<SubscriptionOptions> {
            every { iterator() } returns options.iterator()
        }
        val product = mockk<StoreProduct> {
            every { this@mockk.subscriptionOptions } returns subscriptionOptions
            every { this@mockk.defaultOption } returns defaultOption
        }

        return mockk<Package> {
            every { this@mockk.product } returns product
            every { identifier } returns "\$rc_monthly"
        }
    }

    private fun createPackageWithoutSubscriptionOptions(): Package {
        val product = mockk<StoreProduct> {
            every { subscriptionOptions } returns null
            every { defaultOption } returns null
        }

        return mockk<Package> {
            every { this@mockk.product } returns product
            every { identifier } returns "\$rc_monthly"
        }
    }

    private fun createGoogleSubscriptionOption(offerId: String): GoogleSubscriptionOption {
        val pricingPhase = PricingPhase(
            billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = null,
            price = Price(amountMicros = 9990000, currencyCode = "USD", formatted = "$9.99"),
        )
        return mockk<GoogleSubscriptionOption> {
            every { this@mockk.offerId } returns offerId
            every { id } returns "base-plan:$offerId"
            every { pricingPhases } returns listOf(pricingPhase)
            every { tags } returns emptyList()
            every { isBasePlan } returns false
            every { billingPeriod } returns Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
            every { isPrepaid } returns false
            every { fullPricePhase } returns pricingPhase
            every { freePhase } returns null
            every { introPhase } returns null
        }
    }
}
