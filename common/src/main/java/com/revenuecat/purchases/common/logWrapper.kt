package com.revenuecat.purchases.common

import android.util.Log
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.strings.Emojis

var currentLogHandler: LogHandler = DefaultLogHandler()

private class DefaultLogHandler : LogHandler {
    override fun v(tag: String, msg: String) {
        Log.v(tag, msg)
    }

    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, msg, throwable)
        } else {
            Log.e(tag, msg)
        }
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
    AMAZON_ERROR(listOf(Emojis.BOX, Emojis.DOUBLE_EXCLAMATION)),
}
