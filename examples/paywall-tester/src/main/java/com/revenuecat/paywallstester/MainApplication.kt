package com.revenuecat.paywallstester

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Purchases.logLevel = LogLevel.VERBOSE

        val configurePurchases = ConfigurePurchasesUseCase(this)
        configurePurchases(Constants.GOOGLE_API_KEY_A)
    }
}
