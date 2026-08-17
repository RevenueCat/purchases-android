package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

/**
 * Maps a Google Mobile Ads precision to the RevenueCat equivalent.
 *
 * The `else` branch covers [PrecisionType.UNKNOWN] as well as any constant added after the pinned
 * SDK: this module ships as a precompiled artifact, so an app resolving a newer Google Mobile Ads
 * SDK could deliver a precision this binary was never compiled against, and without the branch
 * that would throw `NoWhenBranchMatchedException` inside the paid-event callback.
 * `AdMobPrecisionMappingTest` still fails once the pinned SDK gains a constant, so the mapping
 * cannot silently fall behind it.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun PrecisionType.toAdRevenuePrecision(): AdRevenuePrecision = when (this) {
    PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
    PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
    PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
    else -> AdRevenuePrecision.UNKNOWN
}
