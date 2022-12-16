package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.Config.debugLogsEnabled
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
        var debugMessage: String? = null
        var infoMessage: String? = null
        var warningMessage: String? = null
        var errorMessage: String? = null
        var errorThrowable: Throwable? = null

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
        previousHandler = currentVerboseLogHandler
        currentVerboseLogHandler = handler
    }

    @After
    fun tearDown() {
        currentVerboseLogHandler = previousHandler
    }

    @Test
    fun debugWithDebugLogsDisabled() {
        val enabled = debugLogsEnabled
        debugLogsEnabled = false

        debugLog(message)
        assertThat(handler.debugMessage).isNull()

        debugLogsEnabled = enabled
    }

    @Test
    fun debugWithLogsEnabled() {
        val enabled = debugLogsEnabled
        debugLogsEnabled = true

        debugLog(message)
        assertThat(handler.debugMessage).isEqualTo(message)

        debugLogsEnabled = enabled
    }

    @Test
    fun info() {
        infoLog(message)
        assertThat(handler.infoMessage).isEqualTo(message)
    }

    @Test
    fun warning() {
        warnLog(message)
        assertThat(handler.warningMessage).isEqualTo(message)
    }

    @Test
    fun errorWithNoThrowable() {
        errorLog(message)
        assertThat(handler.errorMessage).isEqualTo(message)
        assertThat(handler.errorThrowable).isNull()
    }

    @Test
    fun errorWithThrowable() {
        val throwable = ClassNotFoundException()

        errorLog(message, throwable)
        assertThat(handler.errorMessage).isEqualTo(message)
        assertThat(handler.errorThrowable).isSameAs(throwable)
    }
}
