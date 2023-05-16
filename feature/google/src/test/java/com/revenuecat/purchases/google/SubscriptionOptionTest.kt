package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.utils.mockProductDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionOptionTest {

    private val productId = "product-id"
    private val basePlanId = "base-plan-id"
    private val offerToken = "mock-token"

    @Test
    fun `SubscriptionOption can find recurring phase with INFINITE_RECURRING`() {
        val productDetails = mockProductDetails()

        val recurringPhase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = 0,
            price = Price(
                formatted = "$9.00",
                amountMicros = 9000000,
                currencyCode = "USD",
            )
        )

        val subscriptionOption = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(recurringPhase),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        assertThat(subscriptionOption.freePhase).isNull()
        assertThat(subscriptionOption.introPhase).isNull()
        assertThat(subscriptionOption.fullPricePhase).isEqualTo(recurringPhase)
        assertThat(subscriptionOption.billingPeriod?.unit).isEqualTo(Period.Unit.MONTH)
        assertThat(subscriptionOption.billingPeriod?.value).isEqualTo(1)
    }

    @Test
    fun `SubscriptionOption can find recurring phase with NON_RECURRING`() {
        val productDetails = mockProductDetails()

        val recurringPhase = PricingPhase(
            billingPeriod = Period.create("P3M"),
            recurrenceMode = RecurrenceMode.NON_RECURRING,
            billingCycleCount = 0,
            price = Price(
                formatted = "$9.00",
                amountMicros = 9000000,
                currencyCode = "USD",
            )
        )

        val subscriptionOption = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(recurringPhase),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        assertThat(subscriptionOption.freePhase).isNull()
        assertThat(subscriptionOption.introPhase).isNull()
        assertThat(subscriptionOption.fullPricePhase).isEqualTo(recurringPhase)
        assertThat(subscriptionOption.isPrepaid).isTrue
    }

    @Test
    fun `SubscriptionOption can find free phase`() {
        val productDetails = mockProductDetails()

        val freePhase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.NON_RECURRING,
            billingCycleCount = 1,
            price = Price(
                formatted = "",
                amountMicros = 0L,
                currencyCode = "",
            )
        )

        val recurringPhase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = 0,
            price = Price(
                formatted = "$9.00",
                amountMicros = 9000000,
                currencyCode = "USD",
            )
        )

        val subscriptionOption = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(freePhase, recurringPhase),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        assertThat(subscriptionOption.freePhase).isEqualTo(freePhase)
        assertThat(subscriptionOption.introPhase).isNull()
        assertThat(subscriptionOption.fullPricePhase).isEqualTo(recurringPhase)
    }

    @Test
    fun `SubscriptionOption can find intro phase`() {
        val productDetails = mockProductDetails()

        val introPhase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.NON_RECURRING,
            billingCycleCount = 1,
            price = Price(
                formatted = "$1.00",
                amountMicros = 1000000L,
                currencyCode = "USD",
            )
        )

        val recurringPhase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = 0,
            price = Price(
                formatted = "$9.00",
                amountMicros = 9000000,
                currencyCode = "USD",
            )
        )

        val subscriptionOption = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(introPhase, recurringPhase),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        assertThat(subscriptionOption.freePhase).isNull()
        assertThat(subscriptionOption.introPhase).isEqualTo(introPhase)
        assertThat(subscriptionOption.fullPricePhase).isEqualTo(recurringPhase)
    }
}