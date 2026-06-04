package com.revenuecat.sample.vanilla

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.sample.vanilla.data.Constants

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initializeRevenueCat()
        initializeAdMob()
    }

    private fun initializeRevenueCat() {
        Purchases.logLevel = LogLevel.DEBUG

        val configuration = PurchasesConfiguration.Builder(
            context = this,
            apiKey = Constants.REVENUECAT_API_KEY,
        ).build()

        Purchases.configure(configuration)

        Log.d(TAG, "RevenueCat SDK initialized. App user ID: ${Purchases.sharedInstance.appUserID}")
    }

    private fun initializeAdMob() {
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(TAG, "AdMob adapter $adapterClass: ${status?.description}")
            }
            Log.d(TAG, "AdMob SDK initialized successfully")
        }
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}
