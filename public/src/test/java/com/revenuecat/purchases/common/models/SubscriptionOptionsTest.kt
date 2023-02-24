package com.revenuecat.purchases.common.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.utils.mockProductDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionOptionsTest {
    private val discountTag = "discount"

    private val productDetails = mockProductDetails()
    private val purchasingData = GooglePurchasingData.Subscription(
        productId = "product_id",
        productDetails = productDetails,
        optionId = "subscriptionOptionId",
        token = "mock-token"
    )

    private val finalPricingPhase = PricingPhase(
        billingPeriod = Period.create("P1M"),
        recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
        billingCycleCount = 0,
        price = Price(
            formatted = "$4.99",
            amountMicros = 4990000L,
            currencyCode = "USD",
        )
    )

    private val subscriptionOptionFreeTrial = GoogleSubscriptionOption(
        id = "subscriptionOptionId",
        pricingPhases = listOf(
            PricingPhase(
                billingPeriod = Period.create("P1W"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = Price(
                    formatted = "$0.00",
                    amountMicros = 0L,
                    currencyCode = "USD",
                )
            ),
            finalPricingPhase
        ),
        tags = listOf(discountTag),
        purchasingData = purchasingData
    )

    private val subscriptionOptionIntroTrial = GoogleSubscriptionOption(
        id = "subscriptionOptionId",
        pricingPhases = listOf(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = Price(
                    formatted = "$2.99",
                    amountMicros = 2990000L,
                    currencyCode = "USD",
                )
            ),
            finalPricingPhase
        ),
        tags = listOf(discountTag),
        purchasingData = purchasingData
    )

    private val subscriptionOptionBase = GoogleSubscriptionOption(
        id = "subscriptionOptionId",
        pricingPhases = listOf(
            finalPricingPhase
        ),
        tags = emptyList(),
        purchasingData = purchasingData
    )

    private val subscriptionOptions = SubscriptionOptions(listOf(subscriptionOptionBase, subscriptionOptionFreeTrial, subscriptionOptionIntroTrial))

    @Test
    fun `Can find free trial`() {
        val freeTrial = subscriptionOptions.freeTrial
        assertThat(freeTrial).isNotNull
        assertThat(freeTrial?.pricingPhases?.first()?.price?.amountMicros).isEqualTo(0)
    }

    @Test
    fun `Can find intro trial`() {
        val introTrial = subscriptionOptions.introTrial
        assertThat(introTrial).isNotNull
        assertThat(introTrial?.pricingPhases?.first()?.price?.amountMicros).isEqualTo(2990000L)
    }

    @Test
    fun `Can find tags`() {
        val options = subscriptionOptions.withTag(discountTag)
        assertThat(options.size).isEqualTo(2)
    }
}
