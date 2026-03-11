package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.samsung.android.sdk.iap.lib.constants.HelperDefine

internal fun String.createRevenueCatProductTypeFromSamsungIAPTypeString(): ProductType {
    return when (this.lowercase()) {
        HelperDefine.PRODUCT_TYPE_ITEM -> ProductType.INAPP
        HelperDefine.PRODUCT_TYPE_SUBSCRIPTION -> ProductType.SUBS
        else -> {
            log(LogIntent.GALAXY_WARNING) {
                GalaxyStrings.UNKNOWN_GALAXY_IAP_TYPE_STRING.format(this)
            }
            ProductType.UNKNOWN
        }
    }
}
