package com.revenuecat.sample.vanilla.ui.ads

import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision

/** Maps LevelPlay's impression-level revenue precision to RevenueCat's precision values. */
internal fun String?.toAdRevenuePrecision(): AdRevenuePrecision = when (this?.uppercase()) {
    "BID" -> AdRevenuePrecision.EXACT
    "RATE" -> AdRevenuePrecision.PUBLISHER_DEFINED
    "CPM" -> AdRevenuePrecision.ESTIMATED
    else -> AdRevenuePrecision.UNKNOWN
}
