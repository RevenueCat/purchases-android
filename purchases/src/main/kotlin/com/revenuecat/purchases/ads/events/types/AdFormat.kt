package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Common ad format types.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmInline
public value class AdFormat internal constructor(internal val value: String) {
    public companion object {
        public val OTHER: AdFormat = AdFormat("other")
        public val BANNER: AdFormat = AdFormat("banner")
        public val INTERSTITIAL: AdFormat = AdFormat("interstitial")
        public val REWARDED: AdFormat = AdFormat("rewarded")
        public val REWARDED_INTERSTITIAL: AdFormat = AdFormat("rewarded_interstitial")
        public val NATIVE: AdFormat = AdFormat("native")
        public val APP_OPEN: AdFormat = AdFormat("app_open")
        public val MREC: AdFormat = AdFormat("mrec")

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
