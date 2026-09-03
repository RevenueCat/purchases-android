package com.revenuecat.checkpointssample

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.VERBOSE
        Purchases.configure(
            PurchasesConfiguration.Builder(this, Constants.API_KEY)
                .appUserID(null)
                .build(),
        )
    }
}
