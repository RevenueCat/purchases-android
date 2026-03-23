package com.revenuecat.sample.vanilla.data

import com.revenuecat.sample.vanilla.BuildConfig

/**
 * Constants for the AdMob Manual Integration Sample app.
 *
 * IMPORTANT: These are test ad unit IDs provided by Google AdMob for development and testing.
 * Replace these with your actual production ad unit IDs before publishing your app.
 */
object Constants {
    /**
     * RevenueCat API Key.
     * Set your key in `local.properties` (gitignored):
     * ```
     * REVENUECAT_API_KEY=your_api_key_here
     * ```
     */
    val REVENUECAT_API_KEY: String = BuildConfig.REVENUECAT_API_KEY

    /**
     * AdMob Test Ad Unit IDs.
     * These are official test IDs provided by Google that always serve test ads.
     * Source: https://developers.google.com/admob/android/test-ads
     */
    object AdMob {
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"

        /**
         * ⚠️ IMPORTANT: Google's test IDs for native ads often fail to load or behave
         * inconsistently. Use production ad unit IDs from your AdMob account for reliable testing.
         */
        const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        const val NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/1044960115"

        /**
         * Intentionally invalid ID to trigger load failures and demonstrate error tracking.
         */
        const val INVALID_AD_UNIT_ID = "invalid-ad-unit-id"
    }
}
