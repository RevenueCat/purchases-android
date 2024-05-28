package com.revenuecat.paywallstester

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Purchases.logLevel = LogLevel.VERBOSE

        Purchases.configure(
            PurchasesConfiguration.Builder(this, Constants.GOOGLE_API_KEY)
                .finishTransactions(true)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
    }
}
