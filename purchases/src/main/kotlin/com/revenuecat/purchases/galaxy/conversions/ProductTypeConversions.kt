package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.ProductType

internal fun ProductType.toGalaxyProductTypeString(): String? {
    return when (this) {
        ProductType.SUBS -> "subscription"
        ProductType.INAPP -> "item"
        ProductType.UNKNOWN -> null
    }
}
