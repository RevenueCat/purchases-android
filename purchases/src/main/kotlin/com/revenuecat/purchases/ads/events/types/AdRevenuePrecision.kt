package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Common ad revenue precision values.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmInline
public value class AdRevenuePrecision internal constructor(internal val value: String) {
    public companion object {
        public val EXACT: AdRevenuePrecision = AdRevenuePrecision("exact")
        public val PUBLISHER_DEFINED: AdRevenuePrecision = AdRevenuePrecision("publisher_defined")
        public val ESTIMATED: AdRevenuePrecision = AdRevenuePrecision("estimated")
        public val UNKNOWN: AdRevenuePrecision = AdRevenuePrecision("unknown")

        public fun fromString(value: String): AdRevenuePrecision {
            return when (value.lowercase().trim()) {
                "exact" -> EXACT
                "publisher_defined" -> PUBLISHER_DEFINED
                "estimated" -> ESTIMATED
                "unknown" -> UNKNOWN
                else -> AdRevenuePrecision(value)
            }
        }
    }
}
