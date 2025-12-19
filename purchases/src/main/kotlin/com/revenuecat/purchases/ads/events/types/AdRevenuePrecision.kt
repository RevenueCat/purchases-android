package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalRevenueCatAPI

/**
 * Common ad revenue precision values.
 */
@ExperimentalRevenueCatAPI
@JvmInline
value class AdRevenuePrecision internal constructor(internal val value: String) {
    companion object {
        val EXACT = AdRevenuePrecision("exact")
        val PUBLISHER_DEFINED = AdRevenuePrecision("publisher_defined")
        val ESTIMATED = AdRevenuePrecision("estimated")
        val UNKNOWN = AdRevenuePrecision("unknown")

        fun fromString(value: String): AdRevenuePrecision {
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
