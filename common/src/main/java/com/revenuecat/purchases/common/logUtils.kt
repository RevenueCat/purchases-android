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
