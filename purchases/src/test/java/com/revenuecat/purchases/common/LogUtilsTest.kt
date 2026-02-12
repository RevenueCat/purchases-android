package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.LogLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogUtilsTest {
    @Test
    fun logLevelWithDebugLogsEnabled() {
        assertThat(LogLevel.debugLogsEnabled(true)).isEqualTo(LogLevel.DEBUG)
        assertThat(LogLevel.debugLogsEnabled(false)).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun logLevelDebugLogsEnabled() {
        assertThat(LogLevel.VERBOSE.debugLogsEnabled).isEqualTo(true)
        assertThat(LogLevel.DEBUG.debugLogsEnabled).isEqualTo(true)
        assertThat(LogLevel.INFO.debugLogsEnabled).isEqualTo(false)
        assertThat(LogLevel.WARN.debugLogsEnabled).isEqualTo(false)
        assertThat(LogLevel.ERROR.debugLogsEnabled).isEqualTo(false)
    }
}
