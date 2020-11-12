package com.revenuecat.purchases.common

import com.revenuecat.purchases.strings.Emojis

fun googleErrorLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.GOOGLE_ERROR, Emojis.DOUBLE_EXCLAMATION)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun infoLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.INFO)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun purchaseLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.PURCHASE)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun rcSuccessLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.RC_SUCCESS)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun rcPurchaseSuccessLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.RC_SUCCESS, Emojis.PURCHASE)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun rcErrorLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.RC_ERROR, Emojis.DOUBLE_EXCLAMATION)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun userLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.APP_USER_ID)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

fun warningLog(level: LogLevel, message: String) {
    val emojiList = listOf(Emojis.WARNING)
    val emojifiedMessage = "${emojiList.joinToString("")} $message"
    if (level == LogLevel.DEBUG) {
        debugLog(emojifiedMessage)
    } else if (level == LogLevel.INFO) {
        log(emojifiedMessage)
    } else {
        errorLog(emojifiedMessage)
    }
}

/**
 * Enum of log levels.
 */
enum class LogLevel {
    /**
     * For logs at the debug level.
     */
    DEBUG,
    /**
     * For logs at the info level.
     */
    INFO,
    /**
     * For logs at the error level.
     */
    ERROR
}
