package com.revenuecat.sample

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
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

        - An applicationContext is required opposed to an activity context.
        Read more about Android Contexts here: https://developer.android.com/reference/android/content/Context
        - The RevenueCat API key for your Google Play app is also required.
        This can be found in via Project Settings > API keys > App specific keys in the RevenueCat dashboard.
        - An appUserID is required when doing a custom entitlement computation implementation.
        Read more about Identifying Users here: https://docs.revenuecat.com/docs/user-ids
         */
        Purchases.configureInCustomEntitlementsComputationMode(
            this,
            Constants.GOOGLE_API_KEY,
            Constants.defaultAppUserID
        )
    }
}
