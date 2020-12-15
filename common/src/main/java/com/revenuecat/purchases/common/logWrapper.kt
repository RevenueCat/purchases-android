package com.revenuecat.purchases.common

import com.revenuecat.purchases.strings.Emojis

fun log(intent: LogIntent, message: String) {
    val emojifiedMessage = "${intent.emojiList.joinToString("")} $message"
    when (intent) {
        LogIntent.GOOGLE_ERROR -> errorLog(emojifiedMessage)
        LogIntent.GOOGLE_INFO -> infoLog(emojifiedMessage)
        LogIntent.INFO -> infoLog(emojifiedMessage)
        LogIntent.DEBUG_INFO -> debugLog(emojifiedMessage)
        LogIntent.PURCHASE -> debugLog(emojifiedMessage)
        LogIntent.RC_ERROR -> errorLog(emojifiedMessage)
        LogIntent.RC_PURCHASE_SUCCESS -> debugLog(emojifiedMessage)
        LogIntent.RC_SUCCESS -> debugLog(emojifiedMessage)
        LogIntent.USER -> debugLog(emojifiedMessage)
        LogIntent.WARNING -> warnLog(emojifiedMessage)
    }
}

/**
 * Enum of emojis for log messages according to intent.
 */
enum class LogIntent(val emojiList: List<String>) {
    /**
     * Emojis for Google error messages.
     */
    GOOGLE_ERROR(listOf(Emojis.GOOGLE_ERROR, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for Google info messages.
     */
    GOOGLE_INFO(listOf(Emojis.GOOGLE_ERROR, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emoji for information messages on the info level.
     */
    INFO(listOf(Emojis.INFO)),
    /**
     * Emoji for information messages on the debug level.
     */
    DEBUG_INFO(listOf(Emojis.INFO)),
    /**
     * Emoji for purchase messages.
     */
    PURCHASE(listOf(Emojis.PURCHASE)),
    /**
     * Emojis for RevenueCat error messages.
     */
    RC_ERROR(listOf(Emojis.RC_ERROR, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for RevenueCat purchase success messages.
     */
    RC_PURCHASE_SUCCESS(listOf(Emojis.RC_SUCCESS, Emojis.PURCHASE)),
    /**
     * Emoji for RevenueCat success messages.
     */
    RC_SUCCESS(listOf(Emojis.RC_SUCCESS)),
    /**
     * Emoji for App User ID related messages.
     */
    USER(listOf(Emojis.APP_USER_ID)),
    /**
     * Emoji for warning messages.
     */
    WARNING(listOf(Emojis.WARNING))
}
