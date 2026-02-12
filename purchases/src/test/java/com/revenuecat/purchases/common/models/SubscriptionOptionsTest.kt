package com.revenuecat.purchases.common.models

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.runners.Parameterized

@RunWith(AndroidJUnit4::class)
class SubscriptionOptionsTest {
    private val discountTag = "discount"

    private val productId = "product_id"
    private val mockToken = "mock-token"

    private val productDetails = mockProductDetails()

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
        productId = productId,
        basePlanId = "subscriptionOptionId",
        offerId = null,
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
        productDetails = productDetails,
        offerToken = mockToken
    )

    private val subscriptionOptionIntroTrial = GoogleSubscriptionOption(
        productId = productId,
        basePlanId = "subscriptionOptionId",
        offerId = null,
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
        productDetails = productDetails,
        offerToken = mockToken
    )

    private val subscriptionOptionBase = GoogleSubscriptionOption(
        productId = productId,
        basePlanId = "subscriptionOptionId",
        offerId = null,
        pricingPhases = listOf(
            finalPricingPhase
        ),
        tags = emptyList(),
        productDetails = productDetails,
        offerToken = mockToken
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
        val introTrial = subscriptionOptions.introOffer
        assertThat(introTrial).isNotNull
        assertThat(introTrial?.pricingPhases?.first()?.price?.amountMicros).isEqualTo(2990000L)
    }

    @Test
    fun `Can find tags`() {
        val options = subscriptionOptions.withTag(discountTag)
        assertThat(options.size).isEqualTo(2)
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
        val subscriptionOptions = SubscriptionOptions(emptyList())
        val actualDays = subscriptionOptions.billingPeriodToDays(Period.create(period))
        assertThat(actualDays).isEqualTo(days)
    }
}