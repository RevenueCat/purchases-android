package com.revenuecat.purchases.common

import com.revenuecat.purchases.strings.Emojis

fun log(intent: LogIntent, message: String) {
    val emojifiedMessage = "${intent.emojiList.joinToString("")} $message"
    when (intent) {
        LogIntent.GOOGLEERROR -> errorLog(emojifiedMessage)
        LogIntent.GOOGLEINFO -> infoLog(emojifiedMessage)
        LogIntent.INFO -> infoLog(emojifiedMessage)
        LogIntent.DEBUGINFO -> debugLog(emojifiedMessage)
        LogIntent.PURCHASE -> debugLog(emojifiedMessage)
        LogIntent.RCERROR -> errorLog(emojifiedMessage)
        LogIntent.RCPURCHASESUCCESS -> debugLog(emojifiedMessage)
        LogIntent.RCSUCCESS -> debugLog(emojifiedMessage)
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
    GOOGLEERROR(listOf(Emojis.GOOGLE_ERROR, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for Google info messages.
     */
    GOOGLEINFO(listOf(Emojis.GOOGLE_ERROR, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emoji for information messages on the info level.
     */
    INFO(listOf(Emojis.INFO)),
    /**
     * Emoji for information messages on the debug level.
     */
    DEBUGINFO(listOf(Emojis.INFO)),
    /**
     * Emoji for purchase messages.
     */
    PURCHASE(listOf(Emojis.PURCHASE)),
    /**
     * Emojis for RevenueCat error messages.
     */
    RCERROR(listOf(Emojis.RC_ERROR, Emojis.DOUBLE_EXCLAMATION)),
    /**
     * Emojis for RevenueCat purchase success messages.
     */
    RCPURCHASESUCCESS(listOf(Emojis.RC_SUCCESS, Emojis.PURCHASE)),
    /**
     * Emoji for RevenueCat success messages.
     */
    RCSUCCESS(listOf(Emojis.RC_SUCCESS)),
    /**
     * Emoji for App User ID related messages.
     */
    USER(listOf(Emojis.APP_USER_ID)),
    /**
     * Emoji for warning messages.
     */
    WARNING(listOf(Emojis.WARNING))
}
