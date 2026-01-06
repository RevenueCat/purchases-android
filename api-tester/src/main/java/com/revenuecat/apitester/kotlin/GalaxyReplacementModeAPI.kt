package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine

@Suppress("unused", "UNUSED_VARIABLE")
private class GalaxyReplacementModeAPI {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun check(mode: GalaxyReplacementMode) {
        when (mode) {
            GalaxyReplacementMode.INSTANT_PRORATED_DATE,
            GalaxyReplacementMode.INSTANT_PRORATED_CHARGE,
            GalaxyReplacementMode.INSTANT_NO_PRORATION,
            GalaxyReplacementMode.DEFERRED,
            -> {}
        }.exhaustive

        val defaultMode: GalaxyReplacementMode = GalaxyReplacementMode.default
        val samsungProrationMode: HelperDefine.ProrationMode = mode.samsungProrationMode
        val fromMode: GalaxyReplacementMode? = GalaxyReplacementMode.fromSamsungProrationMode(
            HelperDefine.ProrationMode.INSTANT_PRORATED_DATE,
        )
    }
}
