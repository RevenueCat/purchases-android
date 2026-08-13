package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun PrecisionType.toAdRevenuePrecision(): AdRevenuePrecision = when (this) {
    PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
    PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
    PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
    PrecisionType.UNKNOWN -> AdRevenuePrecision.UNKNOWN
}
