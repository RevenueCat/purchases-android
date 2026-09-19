package com.revenuecat.sample.vanilla.ui.ads

import com.google.android.gms.ads.AdValue
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

internal fun Int.toAdRevenuePrecision(): AdRevenuePrecision = when (this) {
    AdValue.PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
    AdValue.PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
    AdValue.PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
    else -> AdRevenuePrecision.UNKNOWN
}
