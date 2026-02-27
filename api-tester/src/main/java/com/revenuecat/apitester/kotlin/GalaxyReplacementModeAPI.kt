package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.GalaxyReplacementMode

@Suppress("unused", "UNUSED_VARIABLE")
private class GalaxyReplacementModeAPI {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    fun check(mode: GalaxyReplacementMode) {
        when (mode) {
            GalaxyReplacementMode.INSTANT_PRORATED_DATE,
            GalaxyReplacementMode.INSTANT_PRORATED_CHARGE,
            GalaxyReplacementMode.INSTANT_NO_PRORATION,
            GalaxyReplacementMode.DEFERRED,
            -> {}
        }.exhaustive

        val defaultMode: GalaxyReplacementMode = GalaxyReplacementMode.default
    }
}
