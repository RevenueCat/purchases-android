package com.revenuecat.samplecat.config

import com.revenuecat.samplecat.BuildConfig

/**
 * Configuration constants for the app's RevenueCat settings.
 */
internal object Constants {
    /**
     * The API key for your app from the RevenueCat dashboard.
     * @see <a href="https://app.revenuecat.com">RevenueCat Dashboard</a>
     */
    internal const val API_KEY: String = BuildConfig.REVENUECAT_API_KEY

    /**
     * The entitlement identifier from the RevenueCat dashboard that is activated
     * upon successful in-app purchase for the duration of the purchase.
     */
    internal val entitlementId: String? = BuildConfig.ENTITLEMENT_ID.takeIf { it.isNotBlank() }
}
