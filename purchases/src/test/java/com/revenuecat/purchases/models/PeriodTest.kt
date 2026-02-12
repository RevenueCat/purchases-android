package com.revenuecat.purchases.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeriodTest {
    companion object {
        private val MAX_OFFSET = Offset.offset(0.0001)
    }

    @Test
    fun `create period creates expected period class for months`() {
        val period = Period.create("P1M")
        assertThat(period.value).isEqualTo(1)
        assertThat(period.unit).isEqualTo(Period.Unit.MONTH)
        assertThat(period.iso8601).isEqualTo("P1M")
    }

    @Test
    fun `create period creates expected period class for years`() {
        val period = Period.create("P1Y")
        assertThat(period.value).isEqualTo(1)
        assertThat(period.unit).isEqualTo(Period.Unit.YEAR)
        assertThat(period.iso8601).isEqualTo("P1Y")
    }

    @Test
    fun `create period creates expected period class for weeks`() {
        val period = Period.create("P1W")
        assertThat(period.value).isEqualTo(1)
        assertThat(period.unit).isEqualTo(Period.Unit.WEEK)
        assertThat(period.iso8601).isEqualTo("P1W")
    }

    @Test
    fun `create period creates expected period class for days`() {
        val period = Period.create("P1D")
        assertThat(period.value).isEqualTo(1)
        assertThat(period.unit).isEqualTo(Period.Unit.DAY)
        assertThat(period.iso8601).isEqualTo("P1D")
    }

    @Test
    fun `create period creates expected period class for multiple units`() {
        val period = Period.create("P3M")
        assertThat(period.value).isEqualTo(3)
        assertThat(period.unit).isEqualTo(Period.Unit.MONTH)
        assertThat(period.iso8601).isEqualTo("P3M")
    }

    @Test
    fun `create period creates expected period class for multiple different units`() {
        val period = Period.create("P12W6D")
        assertThat(period.value).isEqualTo(90)
        assertThat(period.unit).isEqualTo(Period.Unit.DAY)
        assertThat(period.iso8601).isEqualTo("P12W6D")
    }

    @Test
    fun `create period creates expected period class for multiple different units with min unit months`() {
        val period = Period.create("P2Y6M")
        assertThat(period.value).isEqualTo(30)
        assertThat(period.unit).isEqualTo(Period.Unit.MONTH)
        assertThat(period.iso8601).isEqualTo("P2Y6M")
    }

    @Test
    fun `valueInMonths is correct for days`() {
        val period = Period.create("P3D")
        assertThat(period.valueInMonths).isCloseTo(0.1, MAX_OFFSET)
    }

    @Test
    fun `valueInMonths is correct for weeks`() {
        val period = Period.create("P2W")
        assertThat(period.valueInMonths).isCloseTo(0.4602, MAX_OFFSET)
    }

    @Test
    fun `valueInMonths is correct for months`() {
        val period = Period.create("P1M")
        assertThat(period.valueInMonths).isEqualTo(1.0, MAX_OFFSET)
    }

    @Test
    fun `valueInMonths is correct for years`() {
        val period = Period.create("P1Y")
        assertThat(period.valueInMonths).isCloseTo(12.0, MAX_OFFSET)
    }

    @Test
    fun `valueInMonths is 0 for unknown`() {
        val period = Period(value = 1, unit = Period.Unit.UNKNOWN, iso8601 = "P1W")
        assertThat(period.valueInMonths).isCloseTo(0.0, MAX_OFFSET)
    }
}
