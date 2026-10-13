package com.revenuecat.sample.vanilla.data

import com.revenuecat.sample.vanilla.BuildConfig

/**
 * Constants for the LevelPlay manual integration sample app.
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
     * Unity's public LevelPlay demo configuration.
     *
     * Override these values in `local.properties` with your own app key and ad unit IDs to get
     * deterministic test-device fill. The public demo configuration can return no fill.
     * Source: https://github.com/ironsource-mobile/Mediation-Demo-Apps
     */
    object LevelPlay {
        val APP_KEY: String = BuildConfig.LEVELPLAY_APP_KEY
        val BANNER_AD_UNIT_ID: String = BuildConfig.LEVELPLAY_BANNER_AD_UNIT_ID
        val INTERSTITIAL_AD_UNIT_ID: String = BuildConfig.LEVELPLAY_INTERSTITIAL_AD_UNIT_ID
        val REWARDED_AD_UNIT_ID: String = BuildConfig.LEVELPLAY_REWARDED_AD_UNIT_ID
        const val INVALID_AD_UNIT_ID = "invalid-ad-unit-id"
    }
}
