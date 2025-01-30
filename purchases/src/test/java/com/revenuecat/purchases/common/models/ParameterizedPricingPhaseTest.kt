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
        val daily: Price,
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
                "P1D",
                Expected(
                    daily = BASE_PRICE,
                    weekly = Price(
                        formatted = "$699.93",
                        amountMicros = 699930000,
                        currencyCode = "USD"
                    ),
                    monthly = Price(
                        formatted = "$2,999.70",
                        amountMicros = 2999700000,
                        currencyCode = "USD"
                    ),
                    yearly = Price(
                        formatted = "$36,496.35",
                        amountMicros = 36496350000,
                        currencyCode = "USD",
                    )
                )
            ),
            arrayOf(
                "P1W",
                Expected(
                    daily = Price(
                        formatted = "$14.28",
                        amountMicros = 14284285,
                        currencyCode = "USD"
                    ),
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
                    daily = Price(
                        formatted = "$3.33",
                        amountMicros = 3333000,
                        currencyCode = "USD"
                    ),
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
                    daily = Price(
                        formatted = "$0.27",
                        amountMicros = 273945,
                        currencyCode = "USD"
                    ),
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

    @Suppress("DEPRECATION")
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
        val actualDaily = phase.pricePerDay(Locale.US)
        val actualWeekly = phase.pricePerWeek(Locale.US)
        val actualMonthly = phase.pricePerMonth(Locale.US)
        val actualYearly = phase.pricePerYear(Locale.US)
        val actualDailyNoLocale = phase.pricePerDay()
        val actualWeeklyNoLocale = phase.pricePerWeek()
        val actualMonthlyNoLocale = phase.pricePerMonth()
        val actualYearlyNoLocale = phase.pricePerYear()
        val actualMonthlyFormatted = phase.formattedPriceInMonths(Locale.US)
        val actualMonthlyFormattedNoLocale = phase.formattedPriceInMonths()

        // Assert
        assertThat(actualDaily).isEqualTo(expected.daily)
        assertThat(actualWeekly).isEqualTo(expected.weekly)
        assertThat(actualMonthly).isEqualTo(expected.monthly)
        assertThat(actualYearly).isEqualTo(expected.yearly)
        assertThat(actualDailyNoLocale.amountMicros).isEqualTo(expected.daily.amountMicros)
        assertThat(actualWeeklyNoLocale.amountMicros).isEqualTo(expected.weekly.amountMicros)
        assertThat(actualMonthlyNoLocale.amountMicros).isEqualTo(expected.monthly.amountMicros)
        assertThat(actualYearlyNoLocale.amountMicros).isEqualTo(expected.yearly.amountMicros)
        assertThat(actualMonthlyFormatted).isEqualTo(expected.monthly.formatted)
        // Hard to test anything else if the locale is not deterministic, besides formatting exactly like the
        // implementation does.
        assertThat(actualMonthlyFormattedNoLocale).isNotBlank()
    }
}
