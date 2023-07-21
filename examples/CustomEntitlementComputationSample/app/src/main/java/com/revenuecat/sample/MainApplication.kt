package com.revenuecat.sample

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.sample.data.Constants

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /*
        Enable debug logs before calling `configure`.
         */
        Purchases.logLevel = LogLevel.DEBUG

        /*
        Initialize the RevenueCat Purchases SDK.

        - appUserID is nil, so an anonymous ID will be generated automatically by the Purchases SDK. Read more about Identifying Users here: https://docs.revenuecat.com/docs/user-ids
        - observerMode is false, so Purchases will automatically handle finishing transactions. Read more about Observer Mode here: https://docs.revenuecat.com/docs/observer-mode
         */
        val builder = PurchasesConfiguration.Builder(
            this,
            Constants.GOOGLE_API_KEY,
            Constants.defaultAppUserID
        )

        Purchases.configure(builder.build())
    }
}
