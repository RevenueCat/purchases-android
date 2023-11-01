package com.revenuecat.purchases.utils

import com.revenuecat.purchases.amazon.createPrice
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import java.util.Locale

abstract class PriceExtensionsPricePerPeriodTest {
    protected val locale: Locale = Locale.US

    protected fun test(
        priceString: String,
        periodString: String,
        expectedString: String,
    ) {
        val price = priceString.createPrice("US")
        val period = Period.create("P$periodString")
        val expected = expectedString.createPrice("US")

        val result = compute(price, period)

        assertThat(result.formatted).isEqualTo(expectedString)
        assertThat(result.currencyCode).isEqualTo(price.currencyCode)
        assertThat(result.amountMicros).isCloseTo(
            expected.amountMicros,
            // Ignore precision errors so we can compare "$3.99" without extra decimals
            Offset.offset((MICROS_MULTIPLIER / 100).toLong())
        )
    }

    protected abstract fun compute(price: Price, period: Period): Price
}
