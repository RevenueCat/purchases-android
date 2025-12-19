package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.ProductType

internal fun String.createRevenueCatProductTypeFromSamsungIAPTypeString(): ProductType {
    return when (this.lowercase()) {
        "item" -> ProductType.INAPP
        "subscription" -> ProductType.SUBS
        else -> {
            log(LogIntent.GALAXY_WARNING) {
                GalaxyStrings.UNKNOWN_GALAXY_IAP_TYPE_STRING.format(this)
            }
            ProductType.UNKNOWN
        }
    }
}

internal fun ProductType.toGalaxyProductTypeString(): String? {
    return when (this) {
        ProductType.SUBS -> "subscription"
        ProductType.INAPP -> "item"
        ProductType.UNKNOWN -> null
    }
}
      