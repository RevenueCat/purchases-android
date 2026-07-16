package com.revenuecat.e2etests

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases

class E2ETestsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configuration is performed in MainActivity, which can read the Maestro launch
        // arguments (delivered as intent extras) to pick the flow, API key, and settings.
        Purchases.logLevel = LogLevel.DEBUG
    }
}
