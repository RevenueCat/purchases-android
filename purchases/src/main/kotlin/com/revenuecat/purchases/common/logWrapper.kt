package com.revenuecat.purchases.common

import android.util.Log
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.strings.Emojis

internal var currentLogHandler: LogHandler = DefaultLogHandler()

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

@Suppress("CyclomaticComplexMethod")
internal inline fun log(intent: LogIntent, crossinline messageBuilder: () -> String) {
    val fullMessageBuilder = { "${intent.emojiList.joinToString("")} ${messageBuilder()}" }

    when (intent) {
        LogIntent.DEBUG -> debugLog(fullMessageBuilder)
        LogIntent.GOOGLE_ERROR -> errorLog { fullMessageBuilder() }
        LogIntent.GOOGLE_WARNING -> warnLog(fullMessageBuilder)
        LogIntent.INFO -> infoLog(fullMessageBuilder)
        LogIntent.PURCHASE -> debugLog(fullMessageBuilder)
        LogIntent.RC_ERROR -> errorLog { fullMessageBuilder() }
        LogIntent.RC_PURCHASE_SUCCESS -> infoLog(fullMessageBuilder)
        LogIntent.RC_SUCCESS -> debugLog(fullMessageBuilder)
        LogIntent.USER -> debugLog(fullMessageBuilder)
        LogIntent.WARNING -> warnLog(fullMessageBuilder)
        LogIntent.AMAZON_WARNING -> warnLog(fullMessageBuilder)
        LogIntent.AMAZON_ERROR -> errorLog { fullMessageBuilder() }
        LogIntent.GALAXY_WARNING -> warnLog(fullMessageBuilder)
        LogIntent.GALAXY_ERROR -> errorLog { fullMessageBuilder() }
    }
}

/**
 * Enum of emojis for log messages according to intent.
 */
@OptIn(InternalRevenueCatAPI::class)
internal enum class LogIntent(val emojiList: List<String>) {
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
    GALAXY_WARNING(listOf(Emojis.STARS, Emojis.DOUBLE_EXCLAMATION)),
    GALAXY_ERROR(listOf(Emojis.STARS, Emojis.DOUBLE_EXCLAMATION)),
}
