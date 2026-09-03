package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoubleExtensionsTest {

    @Test
    fun `preserves a value at the requested precision despite floating point representation`() {
        assertThat(19.99.roundToDecimalPlaces(2)).isEqualTo(19.99)
    }

    @Test
    fun `truncates the decimals when necessary`() {
        assertThat(19.99.roundToDecimalPlaces(1)).isEqualTo(19.9)
    }

    @Test
    fun `doesn't fail when rounding to more decimal places than are currently present`() {
        assertThat(19.99.roundToDecimalPlaces(100)).isEqualTo(19.99)
    }

    @Test
    fun `truncates positive values without rounding up`() {
        assertThat(8.379.roundToDecimalPlaces(2)).isEqualTo(8.37)
        assertThat(8.375.roundToDecimalPlaces(2)).isEqualTo(8.37)
    }

    @Test
    fun `truncates to zero and higher decimal precision`() {
        assertThat(12.987.roundToDecimalPlaces(0)).isEqualTo(12.0)
        assertThat(1.2345678.roundToDecimalPlaces(6)).isEqualTo(1.234567)
    }
}
