package com.revenuecat.purchases.common

import com.revenuecat.purchases.strings.Emojis

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
    /**
     * Emoji for information messages on the debug level.
     */
    DEBUG(listOf(Emojis.INFO)),
    /**
     * Emojis for Google error messages.
     */
    GOOGLE_ERROR(listOf(Emojis.ROBOT, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for Google info messages.
     */
    GOOGLE_WARNING(listOf(Emojis.ROBOT, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emoji for information messages on the info level.
     */
    INFO(listOf(Emojis.INFO)),
    /**
     * Emoji for purchase messages.
     */
    PURCHASE(listOf(Emojis.MONEY_BAG)),
    /**
     * Emojis for RevenueCat error messages.
     */
    RC_ERROR(listOf(Emojis.SAD_CAT_EYES, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for RevenueCat purchase success messages.
     */
    RC_PURCHASE_SUCCESS(listOf(Emojis.HEART_CAT_EYES, Emojis.MONEY_BAG)),
    /**
     * Emoji for RevenueCat success messages.
     */
    RC_SUCCESS(listOf(Emojis.HEART_CAT_EYES)),
    /**
     * Emoji for App User ID related messages.
     */
    USER(listOf(Emojis.PERSON)),
    /**
     * Emoji for warning messages.
     */
    WARNING(listOf(Emojis.WARNING)),
    /**
     * Emojis for Amazon info messages.
     */
    AMAZON_WARNING(listOf(Emojis.BOX, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for Amazon error messages.
     */
    AMAZON_ERROR(listOf(Emojis.BOX, Emojis.DOUBLE_EXCLAMATION))
}
