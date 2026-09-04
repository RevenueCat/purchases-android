package com.revenuecat.purchases.admob.nextgen

import android.util.Log

internal object Logger {
    private const val TAG = "PurchasesAdMob"

    fun e(message: String) {
        Log.e(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }
}
