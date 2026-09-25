package com.revenuecat.purchases

import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.currentLogHandler
import org.assertj.core.api.Assertions.assertThat

object NoOpLogHandler : LogHandler {
    override fun v(tag: String, msg: String) = Unit
    override fun d(tag: String, msg: String) = Unit
    override fun i(tag: String, msg: String) = Unit
    override fun w(tag: String, msg: String) = Unit
    override fun e(tag: String, msg: String, throwable: Throwable?) = Unit
}

data class LogMessage(val level: LogLevel, val message: String, val throwable: Throwable? = null)

fun captureLogs(block: () -> Unit): List<LogMessage> {
    val previousLogLevel = Config.logLevel
    Config.logLevel = LogLevel.VERBOSE
    val logs = mutableListOf<LogMessage>()
    val previousLogHandler = currentLogHandler
    currentLogHandler = object : LogHandler {
        override fun v(tag: String, msg: String) {
            logs.add(LogMessage(LogLevel.VERBOSE, msg))
        }

        override fun d(tag: String, msg: String) {
            logs.add(LogMessage(LogLevel.DEBUG, msg))
        }

        override fun i(tag: String, msg: String) {
            logs.add(LogMessage(LogLevel.INFO, msg))
        }

        override fun w(tag: String, msg: String) {
            logs.add(LogMessage(LogLevel.WARN, msg))
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            logs.add(LogMessage(LogLevel.ERROR, msg, throwable))
        }
    }
    try {
        block()
    } finally {
        currentLogHandler = previousLogHandler
        Config.logLevel = previousLogLevel
    }
    return logs
}

fun assertLogs(expectedLogMessages: List<LogMessage>, block: () -> Unit) {
    assertThat(captureLogs(block)).containsAll(expectedLogMessages)
}

fun assertLog(logMessage: LogMessage, block: () -> Unit) {
    assertLogs(listOf(logMessage), block)
}

fun assertVerboseLog(logMessage: String, block: () -> Unit) {
    assertLog(LogMessage(LogLevel.VERBOSE, logMessage), block)
}

fun assertDebugLog(logMessage: String, block: () -> Unit) {
    assertLog(LogMessage(LogLevel.DEBUG, logMessage), block)
}

fun assertInfoLog(logMessage: String, block: () -> Unit) {
    assertLog(LogMessage(LogLevel.INFO, logMessage), block)
}

fun assertWarnLog(logMessage: String, block: () -> Unit) {
    assertLog(LogMessage(LogLevel.WARN, logMessage), block)
}

fun assertErrorLog(logMessage: String, throwable: Throwable? = null, block: () -> Unit) {
    assertLog(LogMessage(LogLevel.ERROR, logMessage, throwable), block)
}
