package com.revenuecat.e2etests

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class E2ETestsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure RevenueCat SDK
        Purchases.logLevel = LogLevel.DEBUG

        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = this,
                apiKey = Constants.API_KEY,
            ).build(),
        )
    }
}
