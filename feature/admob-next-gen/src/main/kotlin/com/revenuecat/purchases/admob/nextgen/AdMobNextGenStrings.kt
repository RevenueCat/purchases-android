package com.revenuecat.purchases.admob.nextgen

internal object AdMobNextGenStrings {

    // Configuration
    const val PURCHASES_NOT_CONFIGURED: String =
        "Purchases is not configured. " +
            "Call Purchases.configure() before loading ads to enable RevenueCat ad tracking."

    // Tracking
    const val TRACKING_FAILED: String = "Failed to track a RevenueCat ad event."

    // Placement
    const val PLACEMENT_OVERRIDE_IGNORED: String =
        "Placement override ignored: this ad was not loaded via a loadAndTrack function, " +
            "or its adEventCallback was reassigned directly."
}
