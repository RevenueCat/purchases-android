package com.revenuecat.sample

import android.app.Application
import android.util.Log
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Purchases.debugLogsEnabled = true
        Purchases.configure(this, "LQmxAoIaaQaHpPiWJJayypBDhIpAZCZN")
    }
}

fun showError(message: String) {
    Log.e("Purchases Sample", message)
}

fun showError(error: PurchasesError) {
    showError(error.message)
}