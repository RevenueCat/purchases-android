package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Common ad mediator names.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmInline
value class AdMediatorName internal constructor(internal val value: String) {
    public companion object {
        val AD_MOB = AdMediatorName("AdMob")
        val APP_LOVIN = AdMediatorName("AppLovin")

        public fun fromString(value: String): AdMediatorName {
            return when (value.trim()) {
                "AdMob" -> AD_MOB
                "AppLovin" -> APP_LOVIN
                else -> AdMediatorName(value)
            }
        }
    }
}
