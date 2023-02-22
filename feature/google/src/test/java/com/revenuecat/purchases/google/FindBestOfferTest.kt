package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.toPeriod
import com.revenuecat.purchases.utils.mockPricingPhase
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockSubscriptionOfferDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(AndroidJUnit4::class)
class FindBestOfferTest {
    @Test
    fun `default offer is base plan when no offers`() {
        val basePlanPrice = pricedPhase(4.99, "P1M")
        val productDetail = sub(
            listOf(
                basePlan(
                    "monthly",
                    listOf(basePlanPrice)
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly")
    }

    @Test
    fun `default offer is only free offer`() {
        val basePlanPrice = pricedPhase(4.99, "P1M")
        val productDetail = sub(
            listOf(
                basePlan(
                    "monthly",
                    listOf(basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "month-free",
                    listOf(freePhase("P1M"), basePlanPrice)
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:month-free")
    }

    @Test
    fun `default offer is offer with longest free phase`() {
        val basePlanPrice = pricedPhase(4.99, "P1M")
        val productDetail = sub(
            listOf(
                basePlan(
                    "monthly",
                    listOf(basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "week-free",
                    listOf(freePhase("P1W"), basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "two-week-free",
                    listOf(freePhase("P2W"), basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "month-free",
                    listOf(freePhase("P1M"), basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "nine-day-free",
                    listOf(freePhase("P9D"), basePlanPrice)
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:month-free")
    }

    @Test
    fun `default offer is lowest first phase`() {
        val basePlanPrice = pricedPhase(1000.0, "P1Y")
        val productDetail = sub(
            listOf(
                basePlan(
                    "yearly",
                    listOf(basePlanPrice)
                ),
                offerPlan(
                    "yearly",
                    "one-month-fifty",
                    listOf(pricedPhase(50.0, "P1M"), basePlanPrice)
                ),
                offerPlan(
                    "yearly",
                    "three-months-at-twenty-five",
                    listOf(pricedPhase(25.0, "P1M", 3), basePlanPrice)
                ),
                offerPlan(
                    "yearly",
                    "two-months-at-ninety",
                    listOf(pricedPhase(90.0, "P1M"), basePlanPrice)
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("yearly:three-months-at-twenty-five")
    }

    @Test
    fun `default offer falls back to base plan because rc-ignore-default-offer tag`() {
        val basePlanPrice = pricedPhase(4.99, "P1M")
        val productDetail = sub(
            listOf(
                basePlan(
                    "monthly",
                    listOf(basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "month-free",
                    listOf(freePhase("P1M"), basePlanPrice),
                    true
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly")
    }

    @Test
    fun `default offer is offer with longest free phase without rc-ignore-default-offer tag`() {
        val basePlanPrice = pricedPhase(4.99, "P1M")
        val productDetail = sub(
            listOf(
                basePlan(
                    "monthly",
                    listOf(basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "week-free",
                    listOf(freePhase("P1W"), basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "two-week-free",
                    listOf(freePhase("P2W"), basePlanPrice)
                ),
                offerPlan(
                    "monthly",
                    "month-free",
                    listOf(freePhase("P1M"), basePlanPrice),
                    true
                ),
                offerPlan(
                    "monthly",
                    "nine-day-free",
                    listOf(freePhase("P9D"), basePlanPrice)
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:two-week-free")
    }

    @Test
    fun `default offer is lowest first phase without rc-ignore-default-offer tag`() {
        val basePlanPrice = pricedPhase(1000.0, "P1Y")
        val productDetail = sub(
            listOf(
                basePlan(
                    "yearly",
                    listOf(basePlanPrice)
                ),
                offerPlan(
                    "yearly",
                    "one-month-fifty",
                    listOf(pricedPhase(50.0, "P1M"), basePlanPrice)
                ),
                offerPlan(
                    "yearly",
                    "three-months-at-twenty-five",
                    listOf(pricedPhase(25.0, "P1M", 3), basePlanPrice),
                    true
                ),
                offerPlan(
                    "yearly",
                    "two-months-at-ninety",
                    listOf(pricedPhase(90.0, "P1M"), basePlanPrice)
                )
            )
        )

        val productDetails = listOf(productDetail)
        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("yearly:one-month-fifty")
    }

    private fun sub(offerDetails: List<ProductDetails.SubscriptionOfferDetails>): ProductDetails {
        return mockProductDetails(
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = offerDetails)
    }

    private fun basePlan(
        basePlanId: String,
        pricingPhases: List<ProductDetails.PricingPhase>
    ): ProductDetails.SubscriptionOfferDetails {
        return mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = basePlanId,
            pricingPhases = pricingPhases
        )
    }

    private fun offerPlan(
        basePlanId: String,
        offerId: String,
        pricingPhases: List<ProductDetails.PricingPhase>,
        ignore: Boolean = false
    ): ProductDetails.SubscriptionOfferDetails {
        return mockSubscriptionOfferDetails(
            tags = if (ignore) listOf("rc-ignore-default-offer") else emptyList(),
            offerId = offerId,
            basePlanId = basePlanId,
            pricingPhases = pricingPhases
        )
    }

    private fun freePhase(period: String): ProductDetails.PricingPhase {
        return mockPricingPhase(
            price = 0.0,
            billingPeriod = period,
        )
    }

    private fun pricedPhase(price: Double, period: String, cycleCount: Int = 1): ProductDetails.PricingPhase {
        return mockPricingPhase(
            price = price,
            billingPeriod = period,
            billingCycleCount = cycleCount
        )
    }
}

@RunWith(Parameterized::class)
class PeriodOfferTest(private val period: String, private val days: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf("P1Y", 365),
                arrayOf("P2Y", 730),
                arrayOf("P3M", 90),
                arrayOf("P4D", 4),
                arrayOf("P2W", 14),
                arrayOf("P5X", 0),
                arrayOf("cat", 0)
            )
        }
    }

    @Test
    fun `period to number of days is correct`() {
        val actualDays = billingPeriodToDays(period.toPeriod())
        assertThat(actualDays).isEqualTo(days)
    }
}