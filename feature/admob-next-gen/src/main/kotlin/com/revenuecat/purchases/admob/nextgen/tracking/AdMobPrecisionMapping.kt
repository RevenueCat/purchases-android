package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

/**
 * Maps a Google Mobile Ads precision to the RevenueCat equivalent.
 *
 * The `else` branch is deliberate even though the `when` covers every [PrecisionType] in the
 * currently pinned SDK: this module ships as a precompiled artifact, so an app resolving a newer
 * Google Mobile Ads SDK could deliver a precision constant this binary was never compiled against.
 * Without the branch that would throw `NoWhenBranchMatchedException` inside the paid-event
 * callback. `AdMobPrecisionMappingTest` still fails at build time when a new constant appears, so
 * the mapping cannot silently fall behind the SDK.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun PrecisionType.toAdRevenuePrecision(): AdRevenuePrecision = when (this) {
    PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
    PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
    PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
    else -> AdRevenuePrecision.UNKNOWN
}
