package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Identifies the mechanism that emitted an ad event, so the backend can segment
 * funnel completeness by adapter vs. manual integrations.
 *
 * - [ADAPTER]: auto-captured by an official RevenueCat ad-network adapter.
 * - [MANUAL]: reported via the public `trackAd*` tracking API (native custom
 *   integrations and all hybrid SDKs, which bridge through it).
 *
 * Pre-feature SDK versions send nothing, so the backend defaults to `unknown`.
 */
@InternalRevenueCatAPI
public enum class AdCaptureMethod(public val value: String) {
    ADAPTER("adapter"),
    MANUAL("manual"),
}
