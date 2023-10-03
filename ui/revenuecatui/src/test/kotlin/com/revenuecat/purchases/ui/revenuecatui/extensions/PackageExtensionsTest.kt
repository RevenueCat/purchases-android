package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
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
    fun `introEligibility calculation is true if defaultOption is not base plan`() {
        val rcPackage = createPackage(basePlan = false)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.ELIGIBLE)
    }

    @Test
    fun `introEligibility calculation is false if defaultOption is base plan`() {
        val rcPackage = createPackage(basePlan = true)

        assertThat(rcPackage.introEligibility).isEqualTo(IntroOfferEligibility.INELIGIBLE)
    }

    private fun createPackage(basePlan: Boolean): Package {
        return mockk<Package>().apply {
            every { product } returns mockk<StoreProduct>().apply {
                every { defaultOption } returns mockk<SubscriptionOption>().apply {
                    every { isBasePlan } returns basePlan
                }
            }
        }
    }
}
