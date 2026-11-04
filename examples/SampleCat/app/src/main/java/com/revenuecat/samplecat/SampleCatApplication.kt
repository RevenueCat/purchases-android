package com.revenuecat.samplecat

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.samplecat.config.Constants

/**
 * Application class that initializes the RevenueCat SDK.
 */
class SampleCatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable verbose logging in debug builds
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN

        // Configure the RevenueCat SDK
        Purchases.configure(
            PurchasesConfiguration.Builder(this, Constants.API_KEY).build(),
        )
    }
}
