package com.revenuecat.purchases.common

import android.util.Log
import com.revenuecat.purchases.PurchasesError

fun debugLog(message: String) {
    if (Config.debugLogsEnabled) {
        Log.d("[Purchases] - DEBUG", message)
    }
}

fun log(message: String) {
    Log.w("[Purchases] - INFO", message)
}

fun errorLog(message: String) {
    if (Config.debugLogsEnabled) {
        Log.e("[Purchases] - ERROR", message)
    }
}

fun errorLog(error: PurchasesError) {
    if (Config.debugLogsEnabled) {
        Log.e("[Purchases] - ERROR", error.toString())
    }
}

fun debugLog(emojiList: List<String>, message: String) {
    if (Config.debugLogsEnabled) {
        var len = 1
        for (emoji in emojiList) {
            len += emoji.length
        }
        val emojifiedMessage = emojiList.joinToString("").padEnd(len).plus(message)
        Log.d("[Purchases] - DEBUG", emojifiedMessage)
    }
}

fun log(emojiList: List<String>, message: String) {
    var len = 1
    for (emoji in emojiList) {
        len += emoji.length
    }
    val emojifiedMessage = emojiList.joinToString("").padEnd(len).plus(message)
    Log.w("[Purchases] - INFO", emojifiedMessage)
}

fun errorLog(emojiList: List<String>, message: String) {
    if (Config.debugLogsEnabled) {
        var len = 1
        for (emoji in emojiList) {
            len += emoji.length
        }
        val emojifiedMessage = emojiList.joinToString("").padEnd(len).plus(message)
        Log.e("[Purchases] - ERROR", emojifiedMessage)
    }
}

fun errorLog(emojiList: List<String>, error: PurchasesError) {
    if (Config.debugLogsEnabled) {
        var len = 1
        for (emoji in emojiList) {
            len += emoji.length
        }
        val emojifiedMessage = emojiList.joinToString("").padEnd(len).plus(error.toString())
        Log.e("[Purchases] - ERROR", emojifiedMessage)
    }
}
