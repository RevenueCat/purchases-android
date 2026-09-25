package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Identifies the integration that emitted an ad event, so the backend can segment
 * funnel completeness by adapter implementation vs. manual integrations.
 *
 * - [ADAPTER]: auto-captured by an adapter version that predates adapter-specific values.
 * - [ANDROID_ADMOB_LEGACY_ADAPTER]: auto-captured by the RevenueCat adapter for the legacy
 *   Google Mobile Ads SDK.
 * - [ANDROID_ADMOB_NEXT_GEN_ADAPTER]: auto-captured by the RevenueCat adapter for the Next-Gen
 *   Google Mobile Ads SDK.
 * - [MANUAL]: reported via the public `trackAd*` tracking API (native custom
 *   integrations and all hybrid SDKs, which bridge through it).
 *
 * SDK versions predating capture methods send nothing, so the backend defaults to `unknown`.
 */
@InternalRevenueCatAPI
public enum class AdCaptureMethod(public val value: String) {
    @Deprecated("Use the adapter-specific capture method")
    ADAPTER("adapter"),
    ANDROID_ADMOB_LEGACY_ADAPTER("android_admob_legacy_adapter"),
    ANDROID_ADMOB_NEXT_GEN_ADAPTER("android_admob_next_gen_adapter"),
    MANUAL("manual"),
}
