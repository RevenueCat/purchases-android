package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.common.Config.logLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class LogHandlerTest {
    private class Handler : LogHandler {
        var verboseMessage: String? = null
        var debugMessage: String? = null
        var infoMessage: String? = null
        var warningMessage: String? = null
        var errorMessage: String? = null
        var errorThrowable: Throwable? = null

        override fun v(tag: String, msg: String) {
            verboseMessage = msg
        }

        override fun d(tag: String, msg: String) {
            debugMessage = msg
        }

        override fun i(tag: String, msg: String) {
            infoMessage = msg
        }

        override fun w(tag: String, msg: String) {
            warningMessage = msg
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            errorMessage = msg
            errorThrowable = throwable
        }
    }

    val message = "Logged message"

    private val handler = Handler()
    private lateinit var previousHandler: LogHandler

    @Before
    fun setUp() {
        previousHandler = currentLogHandler
        currentLogHandler = handler
    }

    @After
    fun tearDown() {
        currentLogHandler = previousHandler
    }

    @Test
    fun verboseWithDebugLogs() {
        withChangedLevel(LogLevel.DEBUG) {
            verboseLog(message)
            assertThat(handler.verboseMessage).isNull()
        }
    }

    @Test
    fun debugWithDebugLogsDisabled() {
        withChangedLevel(LogLevel.INFO) {
            debugLog(message)
            assertThat(handler.debugMessage).isNull()
        }
    }

    @Test
    fun debugWithDebugLogsEnabled() {
        withChangedLevel(LogLevel.DEBUG) {
            debugLog(message)
            assertThat(handler.debugMessage).isEqualTo(message)
        }
    }

    @Test
    fun debugWithVerboseLogs() {
        withChangedLevel(LogLevel.VERBOSE) {
            debugLog(message)
            assertThat(handler.debugMessage).isEqualTo(message)
        }
    }

    @Test
    fun info() {
        withChangedLevel(LogLevel.INFO) {
            infoLog(message)
            assertThat(handler.infoMessage).isEqualTo(message)
        }
    }

    @Test
    fun warning() {
        withChangedLevel(LogLevel.WARN) {
            warnLog(message)
            assertThat(handler.warningMessage).isEqualTo(message)
        }
    }

    @Test
    fun errorWithNoThrowable() {
        withChangedLevel(LogLevel.ERROR) {
            errorLog(message)
            assertThat(handler.errorMessage).isEqualTo(message)
            assertThat(handler.errorThrowable).isNull()
        }
    }

    @Test
    fun errorWithThrowable() {
        withChangedLevel(LogLevel.ERROR) {
            val throwable = ClassNotFoundException()

            errorLog(message, throwable)
            assertThat(handler.errorMessage).isEqualTo(message)
            assertThat(handler.errorThrowable).isSameAs(throwable)
        }
    }

    @Test
    fun errorWithDebugLogs() {
        withChangedLevel(LogLevel.DEBUG) {
            val throwable = ClassNotFoundException()

            errorLog(message, throwable)
            assertThat(handler.errorMessage).isEqualTo(message)
            assertThat(handler.errorThrowable).isSameAs(throwable)
        }
    }

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

    private fun withChangedLevel(newLevel: LogLevel, test: () -> Unit) {
        val level = logLevel
        logLevel = newLevel

        test.invoke()

        logLevel = level
    }
}
