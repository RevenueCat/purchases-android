package com.revenuecat.purchases.utils

import com.revenuecat.purchases.models.Price
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class PriceExtensionsNumDecimalsTests(private val price: Price, private val expected: Int) {

    companion object {
        @JvmStatic
        @Parameters
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                Price(
                    amountMicros = 1_000_000,
                    currencyCode = "USD",
                    formatted = "   $ 1.00   ",
                ),
                2,
            ),
            arrayOf(
                Price(
                    amountMicros = 1_000_000,
                    currencyCode = "USD",
                    formatted = "   $ 1    ",
                ),
                2,
            ),
            arrayOf(
                Price(
                    amountMicros = 20000_000_000,
                    currencyCode = "JPY",
                    formatted = "    ¥  20,000    ",
                ),
                0,
            ),
            arrayOf(
                Price(
                    amountMicros = 20000_000_000,
                    currencyCode = "EUR",
                    formatted = "    €  20.000,00    ",
                ),
                2,
            ),
            arrayOf(
                Price(
                    amountMicros = 20000_000_000,
                    currencyCode = "USD",
                    formatted = "    $  20,000.00    ",
                ),
                2,
            ),
            arrayOf(
                Price(
                    amountMicros = 20000_000_000,
                    currencyCode = "EUR",
                    formatted = "    €  20.000,000    ",
                ),
                3,
            ),
            arrayOf(
                Price(
                    amountMicros = 20000_000_000,
                    currencyCode = "USD",
                    formatted = "    $  20,000.000    ",
                ),
                3,
            ),
        )
    }

    @Test
    fun `Should correctly determine number of decimals`() {
        // Arrange, Act
        val actual = price.numDecimals

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

}
