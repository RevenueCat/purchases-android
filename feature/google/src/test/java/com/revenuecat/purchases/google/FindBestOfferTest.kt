package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.utils.mockPricingPhase
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockSubscriptionOfferDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FindBestOfferTest {
    @Test
    fun `default offer is base plan when no offers`() {
        /*
         * sub_1
         *   |
         *   |-> monthly (P1M, $4.99)
         */

        // Base plan
        val monthlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 4.99,
                billingPeriod = "P1M"
            ))
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(monthlyBasePlan))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly")
    }

    @Test
    fun `default offer is only free offer`() {
        /*
         * sub_1
         *   |
         *   |-> monthly (P1M, $4.99)
         *         |
         *         |-> monthly-offer-free
         *               |-> (P1M, free) - BEST OFFER
         */

        // Base plan
        var monthlyBasePlanPricingPhase = mockPricingPhase(
            price = 4.99,
            billingPeriod = "P1M"
        )
        val monthlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "monthly",
            pricingPhases = listOf(monthlyBasePlanPricingPhase)
        )

        // monthly-offer-free (P1M, free)
        val freeMonthPhase = mockPricingPhase(
            price = 0.0,
            billingPeriod = "P1M",
            billingCycleCount = 1,
            recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
        )
        val freeOffer = mockSubscriptionOfferDetails(
            offerId = "monthly-offer-free",
            basePlanId = "monthly",
            pricingPhases = listOf(freeMonthPhase, monthlyBasePlanPricingPhase)
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(monthlyBasePlan, freeOffer))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:monthly-offer-free")
    }

    @Test
    fun `default offer is offer with longest free phase`() {
        /*
         * sub_1
         *   |
         *   |-> monthly (P1M, $4.99)
         *   |     |
         *   |     |-> weekly-offer-free
         *   |     |     |-> (P1W, free)
         *   |     |-> two-week-offer-free
         *   |     |     |-> (P2W, free)
         *   |     |-> monthly-offer-free (BEST OFFER)
         *   |     |     |-> (P1M, free)
         *   |     |-> nine-day-offer-free
         *   |           |-> (P1W, free
         *   |
         *   |-> yearly (P1M, $4.99)
         */

        // Base plan
        var monthlyBasePlanPricingPhase = mockPricingPhase(
            price = 4.99,
            billingPeriod = "P1M"
        )
        val monthlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "monthly",
            pricingPhases = listOf(monthlyBasePlanPricingPhase)
        )

        // weekly-offer-free (P1M, free)
        val weeklyOfferFree = mockSubscriptionOfferDetails(
            offerId = "weeekly-offer-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P1W",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // two-week-offer-free (P2W, free)
        val twoWeekOfferFree = mockSubscriptionOfferDetails(
            offerId = "two-week-offer-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P2W",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // nine-day-offer-free (P2W, free)
        val nineDayOfferFree = mockSubscriptionOfferDetails(
            offerId = "nine-da-offer-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P9D",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // monthly-offer-free (P1M, free)
        val monthlyFreeOffer = mockSubscriptionOfferDetails(
            offerId = "monthly-offer-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P1M",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(
                monthlyBasePlan, weeklyOfferFree, twoWeekOfferFree, monthlyFreeOffer, nineDayOfferFree
            ))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:monthly-offer-free")
    }
}
