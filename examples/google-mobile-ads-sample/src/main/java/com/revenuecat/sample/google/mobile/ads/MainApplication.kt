package com.revenuecat.sample.google.mobile.ads

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.sample.google.mobile.ads.data.Constants

/**
 * Main application class for the Google Mobile Ads Integration Sample.
 *
 * This class demonstrates:
 * 1. Initializing the RevenueCat SDK
 * 2. Initializing the Google Mobile Ads SDK (AdMob)
 * 3. Configuring debug logging for both SDKs
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initializeRevenueCat()
        initializeGoogleMobileAds()
    }

    /**
     * Initialize RevenueCat SDK
     *
     * The SDK is configured with:
     * - Debug log level for development (shows all ad tracking events in logs)
     * - API key from Constants
     * - Automatic user ID generation
     */
    private fun initializeRevenueCat() {
        // Enable debug logging to see all ad events in Logcat
        Purchases.logLevel = LogLevel.DEBUG

        // Configure and initialize RevenueCat
        val configuration = PurchasesConfiguration.Builder(
            context = this,
            apiKey = Constants.REVENUECAT_API_KEY,
        ).build()

        Purchases.configure(configuration)

        // Log the current app user ID
        val appUserId = Purchases.sharedInstance.appUserID
        Log.d(TAG, "RevenueCat SDK initialized successfully")
        Log.d(TAG, "📱 Current App User ID: $appUserId")
    }

    /**
     * Initialize Google Mobile Ads SDK
     *
     * This initializes the SDK asynchronously. Once initialization is complete,
     * ads can be loaded and displayed.
     */
    private fun initializeGoogleMobileAds() {
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(TAG, "Google Mobile Ads adapter $adapterClass: ${status?.description}")
            }
            Log.d(TAG, "Google Mobile Ads SDK initialized successfully")
        }
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}
