package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun GalaxyReplacementMode.toSamsungProrationMode(): HelperDefine.ProrationMode {
    return when (this) {
        GalaxyReplacementMode.INSTANT_PRORATED_DATE -> HelperDefine.ProrationMode.INSTANT_PRORATED_DATE
        GalaxyReplacementMode.INSTANT_PRORATED_CHARGE -> HelperDefine.ProrationMode.INSTANT_PRORATED_CHARGE
        GalaxyReplacementMode.INSTANT_NO_PRORATION -> HelperDefine.ProrationMode.INSTANT_NO_PRORATION
        GalaxyReplacementMode.DEFERRED -> HelperDefine.ProrationMode.DEFERRED
    }
}
