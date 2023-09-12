package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.util.Log

internal object Logger {
    private const val TAG = "RevenueCatUI"

    // TODO-PAYWALLS, allow hooking up a custom log handler
    fun e(message: String) {
        Log.e(TAG, message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }
}
