package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Common ad mediator names.
 */
@InternalRevenueCatAPI
@JvmInline
value class AdMediatorName internal constructor(internal val value: String) {
    companion object {
        val AD_MOB = AdMediatorName("AdMob")
        val APP_LOVIN = AdMediatorName("AppLovin")

        fun fromString(value: String): AdMediatorName {
            return when (value.trim()) {
                "AdMob" -> AD_MOB
                "AppLovin" -> APP_LOVIN
                else -> AdMediatorName(value)
            }
        }
    }
}
