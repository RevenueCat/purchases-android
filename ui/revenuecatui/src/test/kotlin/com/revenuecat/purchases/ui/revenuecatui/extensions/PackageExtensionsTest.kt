package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
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
        val rcPackage = createPackage(numberOfOffers = 1)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE)
    }

    @Test
    fun `introEligibility calculation is MULTIPLE_OFFER_ELIGIBLE if defaultOption has trial and discounted price`() {
        val rcPackage = createPackage(numberOfOffers = 2)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.MULTIPLE_OFFER_ELIGIBLE)
    }

    @Test
    fun `introEligibility calculation is INELIGIBLE if defaultOption is base plan`() {
        val rcPackage = createPackage(numberOfOffers = 0)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.INELIGIBLE)
    }

    private fun createPackage(numberOfOffers: Int): Package {
        return mockk<Package>().apply {
            every { product } returns mockk<StoreProduct>().apply {
                every { defaultOption } returns object : SubscriptionOption {
                    override val id: String
                        get() = "id"
                    override val pricingPhases: List<PricingPhase>
                        get() = List(numberOfOffers + 1) { mockk() } // Base plans have one pricing phase
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
