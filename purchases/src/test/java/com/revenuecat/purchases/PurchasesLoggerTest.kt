//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.infoLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasesLoggerTest {
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
            verboseLog { message }
            assertThat(handler.verboseMessage).isNull()
        }
    }

    @Test
    fun debugWithDebugLogsDisabled() {
        withChangedLevel(LogLevel.INFO) {
            debugLog { message }
            assertThat(handler.debugMessage).isNull()
        }
    }

    @Test
    fun debugWithDebugLogsEnabled() {
        withChangedLevel(LogLevel.DEBUG) {
            debugLog { message }
            assertThat(handler.debugMessage).isEqualTo(message)
        }
    }

    @Test
    fun debugWithVerboseLogs() {
        withChangedLevel(LogLevel.VERBOSE) {
            debugLog { message }
            assertThat(handler.debugMessage).isEqualTo(message)
        }
    }

    @Test
    fun info() {
        withChangedLevel(LogLevel.INFO) {
            infoLog { message }
            assertThat(handler.infoMessage).isEqualTo(message)
        }
    }

    @Test
    fun warning() {
        withChangedLevel(LogLevel.WARN) {
            warnLog { message }
            assertThat(handler.warningMessage).isEqualTo(message)
        }
    }

    @Test
    fun errorWithNoThrowable() {
        withChangedLevel(LogLevel.ERROR) {
            errorLog { message }
            assertThat(handler.errorMessage).isEqualTo(message)
            assertThat(handler.errorThrowable).isNull()
        }
    }

    @Test
    fun errorWithThrowable() {
        withChangedLevel(LogLevel.ERROR) {
            val throwable = ClassNotFoundException()

            errorLog(throwable) { message }
            assertThat(handler.errorMessage).isEqualTo(message)
            assertThat(handler.errorThrowable).isSameAs(throwable)
        }
    }

    @Test
    fun errorWithDebugLogs() {
        withChangedLevel(LogLevel.DEBUG) {
            val throwable = ClassNotFoundException()

            errorLog(throwable) { message }
            assertThat(handler.errorMessage).isEqualTo(message)
            assertThat(handler.errorThrowable).isSameAs(throwable)
        }
    }

    private fun withChangedLevel(newLevel: LogLevel, test: () -> Unit) {
        val level = Purchases.logLevel
        Purchases.logLevel = newLevel

        test.invoke()

        Purchases.logLevel = level
    }
}
