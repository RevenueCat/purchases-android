package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.InstallmentsInfo
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageExtensionsTest {

    @Test
    fun `introOfferEligibility is IntroOfferSingle if defaultOption only has a free trial`() {
        val rcPackage = createPackage(freeTrial = true)

        assertThat(rcPackage.introOfferEligibility).isEqualTo(OfferEligibility.IntroOfferSingle)
    }

    @Test
    fun `introOfferEligibility is IntroOfferMultiple if defaultOption has trial and discounted price`() {
        val rcPackage = createPackage(freeTrial = true, introOffer = true)

        assertThat(rcPackage.introOfferEligibility).isEqualTo(OfferEligibility.IntroOfferMultiple)
    }

    @Test
    fun `introOfferEligibility is Ineligible if defaultOption is base plan`() {
        val rcPackage = createPackage()

        assertThat(rcPackage.introOfferEligibility).isEqualTo(OfferEligibility.Ineligible)
    }

    @Test
    fun `calculateOfferEligibility returns PromoOfferSingle when promo offer has single phase`() {
        val promoOption = createSubscriptionOption(freeTrial = true)
        val resolvedOffer = ResolvedOffer.ConfiguredOffer(promoOption)
        val rcPackage = createPackage()

        assertThat(calculateOfferEligibility(resolvedOffer, rcPackage)).isEqualTo(OfferEligibility.PromoOfferSingle)
    }

    @Test
    fun `calculateOfferEligibility returns PromoOfferMultiple when promo offer has multiple phases`() {
        val promoOption = createSubscriptionOption(freeTrial = true, introOffer = true)
        val resolvedOffer = ResolvedOffer.ConfiguredOffer(promoOption)
        val rcPackage = createPackage()

        assertThat(calculateOfferEligibility(resolvedOffer, rcPackage)).isEqualTo(OfferEligibility.PromoOfferMultiple)
    }

    @Test
    fun `calculateOfferEligibility falls back to intro offer when promo has no discount phases`() {
        val promoOption = createSubscriptionOption()
        val resolvedOffer = ResolvedOffer.ConfiguredOffer(promoOption)
        val rcPackage = createPackage(freeTrial = true)

        assertThat(calculateOfferEligibility(resolvedOffer, rcPackage)).isEqualTo(OfferEligibility.IntroOfferSingle)
    }

    @Test
    fun `calculateOfferEligibility returns Ineligible when promo has no phases and package has no intro`() {
        val promoOption = createSubscriptionOption()
        val resolvedOffer = ResolvedOffer.ConfiguredOffer(promoOption)
        val rcPackage = createPackage()

        assertThat(calculateOfferEligibility(resolvedOffer, rcPackage)).isEqualTo(OfferEligibility.Ineligible)
    }

    @Test
    fun `calculateOfferEligibility returns intro eligibility when no resolved offer`() {
        val rcPackage = createPackage(freeTrial = true)

        assertThat(calculateOfferEligibility(null, rcPackage)).isEqualTo(OfferEligibility.IntroOfferSingle)
    }

    @Test
    fun `calculateOfferEligibility returns intro eligibility for NoConfiguration`() {
        val option = createSubscriptionOption(freeTrial = true)
        val resolvedOffer = ResolvedOffer.NoConfiguration(option)
        val rcPackage = createPackage(introOffer = true)

        assertThat(calculateOfferEligibility(resolvedOffer, rcPackage)).isEqualTo(OfferEligibility.IntroOfferSingle)
    }

    private fun createSubscriptionOption(freeTrial: Boolean = false, introOffer: Boolean = false): SubscriptionOption {
        return object : SubscriptionOption {
            override val id: String = "option-id"
            override val pricingPhases: List<PricingPhase> = getPricingPhases(freeTrial, introOffer)
            override val tags: List<String> = emptyList()
            override val presentedOfferingIdentifier: String? = "offering_id"
            override val presentedOfferingContext: PresentedOfferingContext = PresentedOfferingContext("offering_id")
            override val purchasingData: PurchasingData = mockk()
            override val installmentsInfo: InstallmentsInfo? = null
        }
    }

    private fun createPackage(freeTrial: Boolean = false, introOffer: Boolean = false): Package {
        return mockk<Package>().apply {
            every { product } returns mockk<StoreProduct>().apply {
                every { defaultOption } returns object : SubscriptionOption {
                    override val id: String
                        get() = "id"

                    override val pricingPhases: List<PricingPhase> = getPricingPhases(freeTrial, introOffer)

                    override val tags: List<String>
                        get() = listOf("tag_1")
                    override val presentedOfferingIdentifier: String?
                        get() = presentedOfferingContext.offeringIdentifier
                    override val presentedOfferingContext: PresentedOfferingContext
                        get() = PresentedOfferingContext("offering_id")
                    override val purchasingData: PurchasingData
                        get() = mockk()
                    override val installmentsInfo: InstallmentsInfo?
                        get() = null
                }
            }
        }
    }
}

private fun getPricingPhases(freeTrial: Boolean, introOffer: Boolean): List<PricingPhase> {
    val defaultPricing = PricingPhase(
        billingPeriod = Period.create("P1M"),
        recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
        billingCycleCount = null,
        price = Price(
            formatted = "$2.99",
            amountMicros = 2990000L,
            currencyCode = "USD"
        )
    )

    val freeTrialPricing = if (freeTrial) {
        listOf(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.NON_RECURRING,
                billingCycleCount = null,
                price = Price(
                    formatted = "FREE",
                    amountMicros = 0,
                    currencyCode = "USD"
                )
            )
        )
    } else {
        emptyList()
    }

    val introOfferPricing = if (introOffer) {
        listOf(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 2,
                price = Price(
                    formatted = "$1.99",
                    amountMicros = 1990000L,
                    currencyCode = "USD"
                )
            )
        )
    } else {
        emptyList()
    }

    return listOf(defaultPricing) + freeTrialPricing + introOfferPricing
}
