package com.revenuecat.purchases.admob.nextgen

import android.util.Log

internal object Logger {
    private const val TAG = "PurchasesAdMob"

    fun w(message: String) {
        Log.w(TAG, message)
    }
}
