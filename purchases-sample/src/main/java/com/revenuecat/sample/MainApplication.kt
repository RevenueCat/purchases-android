package com.revenuecat.sample

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError



class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        Purchases.debugLogsEnabled = true
        Purchases.configure(this, "my_api_key")
    }
}

fun showError(message: String) {
    Log.e("Purchases Sample", message)
}

fun showError(error: PurchasesError) {
    showError(error.message)
}