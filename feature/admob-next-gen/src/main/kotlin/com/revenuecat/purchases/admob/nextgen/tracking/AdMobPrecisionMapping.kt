package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

/** Maps a Google Mobile Ads precision to the RevenueCat equivalent. */
internal fun PrecisionType.toAdRevenuePrecision(): AdRevenuePrecision = when (this) {
    PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
    PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
    PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
    // Do not make this exhaustive: the module ships precompiled, so an app on a newer Google
    // Mobile Ads SDK can pass a constant this binary never saw, and that would throw here.
    else -> AdRevenuePrecision.UNKNOWN
}
