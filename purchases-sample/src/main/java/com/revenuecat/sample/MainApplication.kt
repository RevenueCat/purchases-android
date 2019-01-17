package com.revenuecat.sample

import android.app.Application
import android.util.Log
import com.revenuecat.purchases.Purchases

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Purchases.debugLogsEnabled = true
        Purchases.configure(this, "my_api_key")
    }
}

fun showError(message: String) {
    Log.e("Purchases Sample", message)
}