package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdValue
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun Int.toAdRevenuePrecision(): AdRevenuePrecision = when (this) {
    AdValue.PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
    AdValue.PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
    AdValue.PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
    else -> AdRevenuePrecision.UNKNOWN
}
