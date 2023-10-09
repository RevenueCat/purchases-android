package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageExtensionsTest {

    @Test
    fun `introEligibility calculation is SINGLE_OFFER_ELIGIBLE if defaultOption only has a free trial`() {
        val rcPackage = createPackage(freeTrial = true)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE)
    }

    @Test
    fun `introEligibility calculation is MULTIPLE_OFFERS_ELIGIBLE if defaultOption has trial and discounted price`() {
        val rcPackage = createPackage(freeTrial = true, introOffer = true)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE)
    }

    @Test
    fun `introEligibility calculation is INELIGIBLE if defaultOption is base plan`() {
        val rcPackage = createPackage()

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.INELIGIBLE)
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
                        get() = "offering_id"
                    override val purchasingData: PurchasingData
                        get() = mockk()
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
