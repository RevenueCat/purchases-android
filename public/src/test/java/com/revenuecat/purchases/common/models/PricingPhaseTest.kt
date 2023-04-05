package com.revenuecat.purchases.common.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.PaymentMode
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
class PricingPhaseTest {
    private val freePrice = Price(
        formatted = "FREE",
        amountMicros = 0,
        currencyCode = "USD",
    )

    private val nonFreePrice = Price(
        formatted = "$2.99",
        amountMicros = 2990000L,
        currencyCode = "USD",
    )

    @Test
    fun `infinite recurring (null billingCycleCount) phase has no payment mode`() {
        // INFINITE_RECURRING will always have a null billingCycleCount
        val phase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = null,
            price = nonFreePrice
        )

        assertThat(phase.paymentMode).isEqualTo(null)
    }

    @Test
    fun `non-recurring (null billingCycleCount) phase has no payment mode`() {
        // NON_RECURRING will always have a null billingCycleCount
        val phase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.NON_RECURRING,
            billingCycleCount = null,
            price = nonFreePrice
        )

        assertThat(phase.paymentMode).isEqualTo(null)
    }

    @Test
    fun `free phase with 1 billing cycle has FREE_TRIAL payment mode`() {
        val phase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 1,
            price = freePrice
        )

        assertThat(phase.paymentMode).isEqualTo(PaymentMode.FREE_TRIAL)
    }

    @Test
    fun `free phase with 2 billing cycles has FREE_TRIAL payment mode`() {
        val phase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 2,
            price = freePrice
        )

        assertThat(phase.paymentMode).isEqualTo(PaymentMode.FREE_TRIAL)
    }

    @Test
    fun `finite recurring phase with 1 billing cycle has PAY_UP_FRONT payment mode`() {
        val phase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 1,
            price = nonFreePrice
        )

        assertThat(phase.paymentMode).isEqualTo(PaymentMode.PAY_UP_FRONT)
    }

    @Test
    fun `finite recurring phase with 2 billing cycles has PAY_AS_YOU_GO payment mode`() {
        val phase = PricingPhase(
            billingPeriod = Period.create("P1M"),
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 2,
            price = nonFreePrice
        )

        assertThat(phase.paymentMode).isEqualTo(PaymentMode.PAY_AS_YOU_GO)
    }
}