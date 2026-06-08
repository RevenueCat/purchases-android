package com.revenuecat.e2etests

import android.app.Application
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class E2ETestsApplication : Application() {
    @OptIn(InternalRevenueCatAPI::class)
    override fun onCreate() {
        super.onCreate()

        // Configure RevenueCat SDK
        Purchases.logLevel = LogLevel.DEBUG

        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = this,
                apiKey = Constants.API_KEY,
            )
                // This app runs as a minified release build in the Maestro e2e CI job (to exercise
                // the SDK's consumer R8 rules), but uses a Test Store API key. Opt in to allow that
                // combination, which the SDK otherwise blocks in non-debuggable builds.
                .dangerousSettings(DangerousSettings.forTestStoreInReleaseBuild())
                .build(),
        )
    }
}
