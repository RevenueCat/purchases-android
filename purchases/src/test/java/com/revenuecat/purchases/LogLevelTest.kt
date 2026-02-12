package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
 class LogLevelTest {
    @Test
    fun testLogLevelComparable() {
        assertThat(com.revenuecat.purchases.LogLevel.VERBOSE).isLessThan(com.revenuecat.purchases.LogLevel.DEBUG)
        assertThat(com.revenuecat.purchases.LogLevel.DEBUG).isLessThan(com.revenuecat.purchases.LogLevel.INFO)
        assertThat(com.revenuecat.purchases.LogLevel.INFO).isLessThan(com.revenuecat.purchases.LogLevel.WARN)
        assertThat(com.revenuecat.purchases.LogLevel.WARN).isLessThan(com.revenuecat.purchases.LogLevel.ERROR)

        assertThat(com.revenuecat.purchases.LogLevel.DEBUG).isGreaterThanOrEqualTo(com.revenuecat.purchases.LogLevel.VERBOSE)
        assertThat(com.revenuecat.purchases.LogLevel.INFO).isGreaterThanOrEqualTo(com.revenuecat.purchases.LogLevel.DEBUG)
        assertThat(com.revenuecat.purchases.LogLevel.WARN).isGreaterThanOrEqualTo(com.revenuecat.purchases.LogLevel.INFO)
        assertThat(com.revenuecat.purchases.LogLevel.ERROR).isGreaterThanOrEqualTo(com.revenuecat.purchases.LogLevel.WARN)
    }
}
