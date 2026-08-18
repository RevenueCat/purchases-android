package com.revenuecat.sample.admob.data

import com.revenuecat.sample.admob.BuildConfig

/**
 * Constants for the AdMob Integration Sample app.
 *
 * Ad unit IDs default to Google's test IDs. Override any of them — plus the AdMob app ID and the
 * RevenueCat API key — via `local.properties` (gitignored); see the app `build.gradle.kts`.
 * Use your own production IDs before shipping a real app.
 */
object Constants {
    /**
     * RevenueCat API key. Set `REVENUECAT_API_KEY` in `local.properties`.
     * Any valid key works — this sample exercises ad-event tracking, not subscriptions.
     */
    val REVENUECAT_API_KEY: String = BuildConfig.REVENUECAT_API_KEY

    object AdMob {
        /**
         * Google's official test ad unit IDs.
         * https://developers.google.com/admob/android/test-ads
         *
         * ⚠️ The native and native-video test IDs load unreliably. For native formats, use your own
         * ad unit, set `ADMOB_APP_ID` to your app ID, and register the device as a test device.
         */
        object TestAdUnits {
            const val BANNER = "ca-app-pub-3940256099942544/9214589741"
            const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
            const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
            const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
            const val NATIVE_VIDEO = "ca-app-pub-3940256099942544/1044960115"
            const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
            const val REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
        }

        val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_AD_UNIT_ID.ifEmpty { TestAdUnits.BANNER }
        val INTERSTITIAL_AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID.ifEmpty { TestAdUnits.INTERSTITIAL }
        val APP_OPEN_AD_UNIT_ID = BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID.ifEmpty { TestAdUnits.APP_OPEN }
        val NATIVE_AD_UNIT_ID = BuildConfig.ADMOB_NATIVE_AD_UNIT_ID.ifEmpty { TestAdUnits.NATIVE }
        val NATIVE_VIDEO_AD_UNIT_ID = BuildConfig.ADMOB_NATIVE_VIDEO_AD_UNIT_ID.ifEmpty { TestAdUnits.NATIVE_VIDEO }
        val REWARDED_AD_UNIT_ID = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.ifEmpty { TestAdUnits.REWARDED }
        val REWARDED_INTERSTITIAL_AD_UNIT_ID =
            BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID.ifEmpty { TestAdUnits.REWARDED_INTERSTITIAL }

        /**
         * Intentionally invalid ID for exercising load-failure handling
         * (AdMob has no official "error" test ID).
         */
        const val INVALID_AD_UNIT_ID = "invalid-ad-unit-id"
    }
}
