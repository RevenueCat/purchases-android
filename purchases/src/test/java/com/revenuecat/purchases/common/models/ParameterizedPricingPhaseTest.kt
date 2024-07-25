package com.revenuecat.purchases.common.models

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class ParameterizedPricingPhaseTest(
    private val billingPeriodIso8601: String,
    private val expected: Expected,
) {

    class Expected(
        val weekly: Price,
        val monthly: Price,
        val yearly: Price,
    )

    companion object {

        private val BASE_PRICE = Price(
            formatted = "$99.99",
            amountMicros = 99990000,
            currencyCode = "USD",
        )

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "P1W",
                Expected(
                    weekly = BASE_PRICE,
                    monthly = Price(
                        formatted = "$434.48",
                        amountMicros = 434480357,
                        currencyCode = "USD"
                    ),
                    yearly = Price(
                        formatted = "$5,213.76",
                        amountMicros = 5213764285,
                        currencyCode = "USD",
                    )
                )
            ),
            arrayOf(
                "P1M",
                Expected(
                    weekly = Price(
                        formatted = "$23.01",
                        amountMicros = 23011397,
                        currencyCode = "USD",
                    ),
                    monthly = BASE_PRICE,
                    yearly = Price(
                        formatted = "$1,199.88",
                        amountMicros = 1199880000,
                        currencyCode = "USD",
                    )
                )
            ),
            arrayOf(
                "P1Y",
                Expected(
                    weekly = Price(
                        formatted = "$1.92",
                        amountMicros = 1917616,
                        currencyCode = "USD",
                    ),
                    monthly = Price(
                        formatted = "$8.33",
                        amountMicros = 8332500,
                        currencyCode = "USD",
                    ),
                    yearly = BASE_PRICE
                )
            ),
        )
    }

    @Test
    fun `should correctly calculate periodic prices for billing period`() {
        // Arrange
        val phase = PricingPhase(
            billingPeriod = Period.create(billingPeriodIso8601),
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 2,
            price = BASE_PRICE
        )

        // Act
        val actualWeekly = phase.pricePerWeek(Locale.US)
        val actualMonthly = phase.pricePerMonth(Locale.US)
        val actualYearly = phase.pricePerYear(Locale.US)
        val actualWeeklyNoLocale = phase.pricePerWeek()
        val actualMonthlyNoLocale = phase.pricePerMonth()
        val actualYearlyNoLocale = phase.pricePerYear()
        val actualMonthlyFormatted = phase.formattedPriceInMonths(Locale.US)
        val actualMonthlyFormattedNoLocale = phase.formattedPriceInMonths()

        // Assert
        assertThat(actualWeekly).isEqualTo(expected.weekly)
        assertThat(actualMonthly).isEqualTo(expected.monthly)
        assertThat(actualYearly).isEqualTo(expected.yearly)
        assertThat(actualWeeklyNoLocale.amountMicros).isEqualTo(expected.weekly.amountMicros)
        assertThat(actualMonthlyNoLocale.amountMicros).isEqualTo(expected.monthly.amountMicros)
        assertThat(actualYearlyNoLocale.amountMicros).isEqualTo(expected.yearly.amountMicros)
        assertThat(actualMonthlyFormatted).isEqualTo(expected.monthly.formatted)
        // Hard to test anything else if the locale is not deterministic, besides formatting exactly like the
        // implementation does.
        assertThat(actualMonthlyFormattedNoLocale).isNotBlank()
    }
}
