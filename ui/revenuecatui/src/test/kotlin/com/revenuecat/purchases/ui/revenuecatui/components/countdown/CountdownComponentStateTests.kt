package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CountdownComponentStateTests {

    @Test
    fun `CountdownTime fromInterval with zero interval returns ZERO`() {
        val result = CountdownTime.fromInterval(0)
        
        assertThat(result).isEqualTo(CountdownTime.ZERO)
        assertThat(result.days).isEqualTo(0)
        assertThat(result.hours).isEqualTo(0)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(0)
    }

    @Test
    fun `CountdownTime fromInterval with negative interval returns ZERO`() {
        val result = CountdownTime.fromInterval(-1000)
        
        assertThat(result).isEqualTo(CountdownTime.ZERO)
    }

    @Test
    fun `CountdownTime fromInterval calculates seconds correctly`() {
        val result = CountdownTime.fromInterval(45_000)
        
        assertThat(result.days).isEqualTo(0)
        assertThat(result.hours).isEqualTo(0)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(45)
    }

    @Test
    fun `CountdownTime fromInterval calculates minutes correctly`() {
        val result = CountdownTime.fromInterval(3_600_000)
        
        assertThat(result.days).isEqualTo(0)
        assertThat(result.hours).isEqualTo(1)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(0)
    }

    @Test
    fun `CountdownTime fromInterval calculates hours correctly`() {
        val result = CountdownTime.fromInterval(7_200_000)
        
        assertThat(result.days).isEqualTo(0)
        assertThat(result.hours).isEqualTo(2)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(0)
    }

    @Test
    fun `CountdownTime fromInterval calculates days correctly`() {
        val result = CountdownTime.fromInterval(86_400_000)
        
        assertThat(result.days).isEqualTo(1)
        assertThat(result.hours).isEqualTo(0)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(0)
    }

    @Test
    fun `CountdownTime fromInterval calculates complex time correctly`() {
        val result = CountdownTime.fromInterval(90_061_000)
        
        assertThat(result.days).isEqualTo(1)
        assertThat(result.hours).isEqualTo(1)
        assertThat(result.minutes).isEqualTo(1)
        assertThat(result.seconds).isEqualTo(1)
    }

    @Test
    fun `CountdownTime fromInterval handles large intervals`() {
        val result = CountdownTime.fromInterval(259_200_000)
        
        assertThat(result.days).isEqualTo(3)
        assertThat(result.hours).isEqualTo(0)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(0)
    }

    @Test
    fun `CountdownTime fromInterval with milliseconds rounds down`() {
        val result = CountdownTime.fromInterval(5_999)
        
        assertThat(result.days).isEqualTo(0)
        assertThat(result.hours).isEqualTo(0)
        assertThat(result.minutes).isEqualTo(0)
        assertThat(result.seconds).isEqualTo(5)
    }

    @Test
    fun `CountdownTime ZERO constant is correct`() {
        val zero = CountdownTime.ZERO
        
        assertThat(zero.days).isEqualTo(0)
        assertThat(zero.hours).isEqualTo(0)
        assertThat(zero.minutes).isEqualTo(0)
        assertThat(zero.seconds).isEqualTo(0)
    }
}
