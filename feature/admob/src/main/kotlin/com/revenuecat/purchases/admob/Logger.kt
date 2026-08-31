package com.revenuecat.purchases.admob

import android.util.Log

internal object Logger {
    private const val TAG = "PurchasesAdMob"

    fun e(message: String) {
        Log.e(TAG, message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun v(message: String) {
        Log.v(TAG, message)
    }
}
