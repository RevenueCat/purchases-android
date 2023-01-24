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
    fun `parsed period for P1Y is 365 days`() {
        val days = parseBillPeriodToDays("P1Y")
        assertThat(days).isEqualTo(365)
    }

    @Test
    fun `parsed period for P2Y is 730 days`() {
        val days = parseBillPeriodToDays("P2Y")
        assertThat(days).isEqualTo(730)
    }

    @Test
    fun `parsed period for P1M is 30 days`() {
        val days = parseBillPeriodToDays("P1M")
        assertThat(days).isEqualTo(30)
    }

    @Test
    fun `parsed period for P2M is 60 days`() {
        val days = parseBillPeriodToDays("P2M")
        assertThat(days).isEqualTo(60)
    }

    @Test
    fun `parsed period for P1W is 7 days`() {
        val days = parseBillPeriodToDays("P1W")
        assertThat(days).isEqualTo(7)
    }

    @Test
    fun `parsed period for P2W is 14 days`() {
        val days = parseBillPeriodToDays("P2W")
        assertThat(days).isEqualTo(14)
    }

    @Test
    fun `parsed period for P1D is 1 day`() {
        val days = parseBillPeriodToDays("P1D")
        assertThat(days).isEqualTo(1)
    }

    @Test
    fun `parsed period for P2D is 2 days`() {
        val days = parseBillPeriodToDays("P2D")
        assertThat(days).isEqualTo(2)
    }

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
         *         |-> month-free (BEST OFFER)
         *               |-> (P1M, free)
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

        // month-free (P1M, free)
        val monthFreeOffer = mockPricingPhase(
            price = 0.0,
            billingPeriod = "P1M",
            billingCycleCount = 1,
            recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
        )
        val freeOffer = mockSubscriptionOfferDetails(
            offerId = "month-free",
            basePlanId = "monthly",
            pricingPhases = listOf(monthFreeOffer, monthlyBasePlanPricingPhase)
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(monthlyBasePlan, freeOffer))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:month-free")
    }

    @Test
    fun `default offer is offer with longest free phase`() {
        /*
         * sub_1
         *   |
         *   |-> monthly (P1M, $4.99)
         *   |     |
         *   |     |-> week-free
         *   |     |     |-> (P1W, free)
         *   |     |-> two-week-free
         *   |     |     |-> (P2W, free)
         *   |     |-> monthly-free (BEST OFFER)
         *   |     |     |-> (P1M, free)
         *   |     |-> nine-day-free
         *   |           |-> (P9D, free
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

        // week-free (P1M, free)
        val weekFreeOffer = mockSubscriptionOfferDetails(
            offerId = "week-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P1W",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // two-week-free (P2W, free)
        val twoWeekFreeOffer = mockSubscriptionOfferDetails(
            offerId = "two-week-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P2W",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // monthly-free (P1M, free)
        val monthlyFreeOffer = mockSubscriptionOfferDetails(
            offerId = "month-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P1M",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // nine-day-free (P2W, free)
        val nineDayFreeOffer = mockSubscriptionOfferDetails(
            offerId = "nine-day-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P9D",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(
                monthlyBasePlan, weekFreeOffer, twoWeekFreeOffer, monthlyFreeOffer, nineDayFreeOffer
            ))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:month-free")
    }

    @Test
    fun `default offer is biggest savings`() {
        /*
         * sub_1
         *   |
         *   |-> yearly (P1Y, $100)
         *   |     |
         *   |     |-> one-month-fifty
         *   |     |     |-> (P1M, $50)
         *   |     |-> three-months-at-twenty-five (BEST OFFER)
         *   |     |     |-> (P1M, $25, 3)
         *   |     |-> two-months-at-ninety
         *   |           |-> (P1M, $90, 2)
         */

        // Base plan
        var yearlyBasePlanPricingPhase = mockPricingPhase(
            price = 1000.0,
            billingPeriod = "P1Y"
        )
        val yearlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "yearly",
            pricingPhases = listOf(yearlyBasePlanPricingPhase)
        )

        // one-month-fifty (P1M, $50)
        val oneMonthAtFiftyOffer = mockSubscriptionOfferDetails(
            offerId = "one-month-fifty",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                price = 50.0,
                billingPeriod = "P1M",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING
            )) + yearlyBasePlanPricingPhase
        )

        // three-months-at-twenty-five (P1M, $25, 3)
        val threeMonthsAtTwentyFiveOffer = mockSubscriptionOfferDetails(
            offerId = "three-months-at-twenty-five",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                price = 25.0,
                billingPeriod = "P1M",
                billingCycleCount = 3,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING
            )) + yearlyBasePlanPricingPhase
        )

        // two-months-at-ninety (P1M, $90, 2)
        val twoMonthsAtNinetyOffer = mockSubscriptionOfferDetails(
            offerId = "two-months-at-ninety",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                price = 90.0,
                billingPeriod = "P1M",
                billingCycleCount = 2,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING
            )) + yearlyBasePlanPricingPhase
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(
                yearlyBasePlan, oneMonthAtFiftyOffer, threeMonthsAtTwentyFiveOffer, twoMonthsAtNinetyOffer
            ))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("yearly:three-months-at-twenty-five")
    }

    @Test
    fun `default offer falls back to base plan because rc-ignore-best-offer tag`() {
        /*
         * sub_1
         *   |
         *   |-> monthly (P1M, $4.99) (BEST OFFER)
         *         |
         *         |-> month-free [rc-ignore-best-offer]
         *               |-> (P1M, free)
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

        // month-free (P1M, free)
        val monthFreeOffer = mockPricingPhase(
            price = 0.0,
            billingPeriod = "P1M",
            billingCycleCount = 1,
            recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
        )
        val freeOffer = mockSubscriptionOfferDetails(
            tags = listOf("rc-ignore-best-offer"),
            offerId = "month-free",
            basePlanId = "monthly",
            pricingPhases = listOf(monthFreeOffer, monthlyBasePlanPricingPhase)
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(monthlyBasePlan, freeOffer))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly")
    }

    @Test
    fun `default offer is offer with longest free phase without rc-ignore-best-offer tag`() {
        /*
         * sub_1
         *   |
         *   |-> monthly (P1M, $4.99)
         *   |     |
         *   |     |-> week-free
         *   |     |     |-> (P1W, free)
         *   |     |-> two-week-free (BEST OFFER)
         *   |     |     |-> (P2W, free)
         *   |     |-> monthly-free [rc-ignore-best-offer]
         *   |     |     |-> (P1M, free)
         *   |     |-> nine-day-free
         *   |           |-> (P9D, free
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

        // week-free (P1M, free)
        val weekFreeOffer = mockSubscriptionOfferDetails(
            offerId = "week-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P1W",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // two-week-free (P2W, free)
        val twoWeekFreeOffer = mockSubscriptionOfferDetails(
            offerId = "two-week-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P2W",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // monthly-free (P1M, free)
        val monthlyFreeOffer = mockSubscriptionOfferDetails(
            tags = listOf("rc-ignore-best-offer"),
            offerId = "month-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P1M",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // nine-day-free (P2W, free)
        val nineDayFreeOffer = mockSubscriptionOfferDetails(
            offerId = "nine-day-free",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                price = 0.0,
                billingPeriod = "P9D",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            )) + monthlyBasePlanPricingPhase
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(
                monthlyBasePlan, weekFreeOffer, twoWeekFreeOffer, monthlyFreeOffer, nineDayFreeOffer
            ))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("monthly:two-week-free")
    }

    @Test
    fun `default offer is biggest savings without rc-ignore-best-offer tag`() {
        /*
         * sub_1
         *   |
         *   |-> yearly (P1Y, $100)
         *   |     |
         *   |     |-> one-month-fifty (BEST OFFER)
         *   |     |     |-> (P1M, $50)
         *   |     |-> three-months-at-twenty-five [rc-ignore-best-offer]
         *   |     |     |-> (P1M, $25, 3)
         *   |     |-> two-months-at-ninety
         *   |           |-> (P1M, $90, 2)
         */

        // Base plan
        var yearlyBasePlanPricingPhase = mockPricingPhase(
            price = 1000.0,
            billingPeriod = "P1Y"
        )
        val yearlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "yearly",
            pricingPhases = listOf(yearlyBasePlanPricingPhase)
        )

        // one-month-fifty (P1M, $50)
        val oneMonthAtFiftyOffer = mockSubscriptionOfferDetails(
            offerId = "one-month-fifty",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                price = 50.0,
                billingPeriod = "P1M",
                billingCycleCount = 1,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING
            )) + yearlyBasePlanPricingPhase
        )

        // three-months-at-twenty-five (P1M, $25, 3)
        val threeMonthsAtTwentyFiveOffer = mockSubscriptionOfferDetails(
            tags = listOf("rc-ignore-best-offer"),
            offerId = "three-months-at-twenty-five",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                price = 25.0,
                billingPeriod = "P1M",
                billingCycleCount = 3,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING
            )) + yearlyBasePlanPricingPhase
        )

        // two-months-at-ninety (P1M, $90, 2)
        val twoMonthsAtNinetyOffer = mockSubscriptionOfferDetails(
            offerId = "two-months-at-ninety",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                price = 90.0,
                billingPeriod = "P1M",
                billingCycleCount = 2,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING
            )) + yearlyBasePlanPricingPhase
        )

        // Product details
        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(
                yearlyBasePlan, oneMonthAtFiftyOffer, threeMonthsAtTwentyFiveOffer, twoMonthsAtNinetyOffer
            ))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        val defaultOption = storeProducts.first().defaultOption as GoogleSubscriptionOption
        assertThat(defaultOption.id).isEqualTo("yearly:one-month-fifty")
    }
}
