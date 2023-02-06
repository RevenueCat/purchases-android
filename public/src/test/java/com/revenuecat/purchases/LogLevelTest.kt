package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
 class LogLevelTest {
    @Test
    fun testLogLevelComparable() {
        assertThat(LogLevel.VERBOSE).isLessThan(LogLevel.DEBUG)
        assertThat(LogLevel.DEBUG).isLessThan(LogLevel.INFO)
        assertThat(LogLevel.INFO).isLessThan(LogLevel.WARN)
        assertThat(LogLevel.WARN).isLessThan(LogLevel.ERROR)

        assertThat(LogLevel.DEBUG).isGreaterThanOrEqualTo(LogLevel.VERBOSE)
        assertThat(LogLevel.INFO).isGreaterThanOrEqualTo(LogLevel.DEBUG)
        assertThat(LogLevel.WARN).isGreaterThanOrEqualTo(LogLevel.INFO)
        assertThat(LogLevel.ERROR).isGreaterThanOrEqualTo(LogLevel.WARN)
    }
}
