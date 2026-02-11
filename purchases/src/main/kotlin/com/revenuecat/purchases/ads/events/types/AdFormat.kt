package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Common ad format types.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmInline
value class AdFormat internal constructor(internal val value: String) {
    public companion object {
        val OTHER = AdFormat("other")
        val BANNER = AdFormat("banner")
        val INTERSTITIAL = AdFormat("interstitial")
        val REWARDED = AdFormat("rewarded")
        val REWARDED_INTERSTITIAL = AdFormat("rewarded_interstitial")
        val NATIVE = AdFormat("native")
        val APP_OPEN = AdFormat("app_open")
        val MREC = AdFormat("mrec")

        public fun fromString(value: String): AdFormat {
            return when (value.trim()) {
                "other" -> OTHER
                "banner" -> BANNER
                "interstitial" -> INTERSTITIAL
                "rewarded" -> REWARDED
                "rewarded_interstitial" -> REWARDED_INTERSTITIAL
                "native" -> NATIVE
                "app_open" -> APP_OPEN
                "mrec" -> MREC
                else -> AdFormat(value)
            }
        }
    }
}
