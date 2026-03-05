package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.galaxy.GalaxyBillingMode

@Suppress("unused", "UNUSED_VARIABLE")
private class GalaxyBillingModeAPI {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun check(galaxyBillingMode: GalaxyBillingMode) {
        when (galaxyBillingMode) {
            GalaxyBillingMode.PRODUCTION,
            GalaxyBillingMode.TEST,
            GalaxyBillingMode.ALWAYS_FAIL,
            -> {
            }
        }.exhaustive
    }
}
