package com.revenuecat.sample.admob.data

/**
 * Constants for the AdMob Integration Sample app.
 *
 * IMPORTANT: These are test ad unit IDs provided by Google AdMob for development and testing.
 * Replace these with your actual production ad unit IDs before publishing your app.
 */
object Constants {
    /**
     * RevenueCat API Key
     * Get your API key from https://app.revenuecat.com/
     *
     * NOTE: For this sample app, you can use any valid RevenueCat API key.
     * The sample demonstrates ad event tracking, not subscription functionality.
     */
    const val REVENUECAT_API_KEY = "YOUR_PUBLIC_API_KEY"

    /**
     * AdMob Test Ad Unit IDs
     * These are official test IDs provided by Google that always serve test ads.
     * Source: https://developers.google.com/admob/android/test-ads
     */
    object AdMob {
        /**
         * Banner Ad Test Unit ID
         * Always successfully loads and displays a test banner ad.
         */
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

        /**
         * Interstitial Ad Test Unit ID
         * Always successfully loads and displays a test interstitial ad.
         */
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        /**
         * Native Video Ad Test Unit ID
         * Test ID for native ads with video content.
         * More reliable than the standard native ad test ID.
         */
        const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/1044960115"

        /**
         * Invalid Ad Unit ID - Used for Error Testing
         * This intentionally invalid ID triggers load failures to demonstrate
         * how to handle and track ad load errors with RevenueCat.
         *
         * NOTE: AdMob does not provide an official "error test ID", so we use
         * an invalid ID to simulate load failures for testing purposes.
         */
        const val INVALID_AD_UNIT_ID = "invalid-ad-unit-id"
    }
}
