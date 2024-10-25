package com.revenuecat.sample

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.amazon.AmazonConfiguration
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
        - purchasesAreCompletedBy is set to REVENUECAT, so Purchases will automatically handle finishing transactions. Read more about finishing transactions here: https://www.revenuecat.com/docs/migrating-to-revenuecat/sdk-or-not/finishing-transactions
         */
        val builder = when (BuildConfig.STORE) {
            "amazon" -> AmazonConfiguration.Builder(this, Constants.AMAZON_API_KEY)
            "google" -> PurchasesConfiguration.Builder(this, Constants.GOOGLE_API_KEY)
            else -> throw IllegalArgumentException("Invalid store.")
        }
        Purchases.configure(
            builder
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
    }
}