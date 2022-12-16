package com.revenuecat.purchases.common

import android.util.Log
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.logging.LogLevel
import com.revenuecat.purchases.logging.VerboseLogHandler
import com.revenuecat.purchases.strings.Emojis

var currentLogHandler: LogHandler = DefaultLogHandler()
var currentVerboseLogHandler: VerboseLogHandler = DefaultVerboseLogHandler()

private class DefaultLogHandler : LogHandler {
    override fun d(tag: String, msg: String) {
        debugLog(msg, null)
    }

    override fun i(tag: String, msg: String) {
        infoLog(msg, null)
    }

    override fun w(tag: String, msg: String) {
        warnLog(msg, null)
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        errorLog(msg, throwable)
    }
}

fun verboseLogHandlerToLogHandler(value: LogHandler) = object : VerboseLogHandler {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        elements: Array<StackTraceElement>?
    ) {
        when (level) {
            LogLevel.DEBUG -> value.d(tag, message)
            LogLevel.INFO -> value.i(tag, message)
            LogLevel.WARN -> value.w(tag, message)
            LogLevel.ERROR -> value.e(tag, message, throwable)
            LogLevel.VERBOSE -> return // LogHandler doesn't handle verbose
        }
    }
}

private class DefaultVerboseLogHandler : VerboseLogHandler {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        elements: Array<StackTraceElement>?
    ) {
        val messageWithStacktraceIfNeeded = message + if (Config.verboseLogs) { stacktraceString(elements) } else ""

        when (level) {
            LogLevel.DEBUG -> Log.d(tag, messageWithStacktraceIfNeeded)
            LogLevel.INFO -> Log.i(tag, messageWithStacktraceIfNeeded)
            LogLevel.WARN -> Log.w(tag, messageWithStacktraceIfNeeded)
            LogLevel.ERROR -> throwable?.let {
                Log.e(tag, messageWithStacktraceIfNeeded, it)
            } ?: Log.e(tag, messageWithStacktraceIfNeeded)
            LogLevel.VERBOSE -> throwable?.let {
                Log.v(tag, messageWithStacktraceIfNeeded, it)
            } ?: Log.v(tag, messageWithStacktraceIfNeeded)
        }
    }

    private fun stacktraceString(elements: Array<StackTraceElement>?) = if (elements != null) {
        "\n${elements.joinToString("\n")}"
    } else {
        // Getting the stacktrace by creating a Throwable is faster and gives the correct item (this function)
        // for the first item of the stacktrace. It prints "java.lang.Throwable\n" in the first line,
        // so we remove that. Using currentThread().stacktrace will print this at the beginning of the stack:
        //      dalvik.system.VMStack.getThreadStackTrace(Native Method)
        //      java.lang.Thread.getStackTrace(Thread.java:1841)
        "\n${Throwable().stackTraceToString().replaceFirst("java.lang.Throwable\n", "")}"
    }
}

fun log(intent: LogIntent, message: String) {
    val fullMessage = "${intent.emojiList.joinToString("")} $message"

    when (intent) {
        LogIntent.DEBUG -> debugLog(fullMessage)
        LogIntent.GOOGLE_ERROR -> errorLog(fullMessage)
        LogIntent.GOOGLE_WARNING -> warnLog(fullMessage)
        LogIntent.INFO -> infoLog(fullMessage)
        LogIntent.PURCHASE -> debugLog(fullMessage)
        LogIntent.RC_ERROR -> errorLog(fullMessage)
        LogIntent.RC_PURCHASE_SUCCESS -> infoLog(fullMessage)
        LogIntent.RC_SUCCESS -> debugLog(fullMessage)
        LogIntent.USER -> debugLog(fullMessage)
        LogIntent.WARNING -> warnLog(fullMessage)
        LogIntent.AMAZON_WARNING -> warnLog(fullMessage)
        LogIntent.AMAZON_ERROR -> errorLog(fullMessage)
    }
}

/**
 * Enum of emojis for log messages according to intent.
 */
enum class LogIntent(val emojiList: List<String>) {
    DEBUG(listOf(Emojis.INFO)),
    GOOGLE_ERROR(listOf(Emojis.ROBOT, Emojis.DOUBLE_EXCLAMATION)),
    GOOGLE_WARNING(listOf(Emojis.ROBOT, Emojis.DOUBLE_EXCLAMATION)),
    INFO(listOf(Emojis.INFO)),
    PURCHASE(listOf(Emojis.MONEY_BAG)),
    RC_ERROR(listOf(Emojis.SAD_CAT_EYES, Emojis.DOUBLE_EXCLAMATION)),
    RC_PURCHASE_SUCCESS(listOf(Emojis.HEART_CAT_EYES, Emojis.MONEY_BAG)),
    RC_SUCCESS(listOf(Emojis.HEART_CAT_EYES)),
    USER(listOf(Emojis.PERSON)),
    WARNING(listOf(Emojis.WARNING)),
    AMAZON_WARNING(listOf(Emojis.BOX, Emojis.DOUBLE_EXCLAMATION)),
    AMAZON_ERROR(listOf(Emojis.BOX, Emojis.DOUBLE_EXCLAMATION))
}
