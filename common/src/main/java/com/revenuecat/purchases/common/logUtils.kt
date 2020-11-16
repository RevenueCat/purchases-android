package com.revenuecat.purchases.common

import android.util.Log
import com.revenuecat.purchases.PurchasesError

fun debugLog(message: String) {
    if (Config.debugLogsEnabled) {
        Log.d("[Purchases] - DEBUG", message)
    }
}

fun infoLog(message: String) {
    Log.i("[Purchases] - INFO", message)
}

fun warnLog(message: String) {
    Log.w("[Purchases] - WARNING", message)
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

/**
 * Enum of log levels.
 */
enum class LogLevel {
    /**
     * For logs at the debug level.
     */
    DEBUG,
    /**
     * For logs at the error level.
     */
    ERROR,
    /**
     * For logs at the info level.
     */
    INFO,
    /**
     * For logs at the warn level.
     */
    WARN
}
