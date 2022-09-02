package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.ProductType

fun String?.toRevenueCatProductType(): ProductType {
    return when (this) {
        BillingClient.SkuType.INAPP -> ProductType.INAPP
        BillingClient.SkuType.SUBS -> ProductType.SUBS
        else -> ProductType.UNKNOWN
    }
}

fun ProductType.toGoogleProductType(): String? {
    return when (this) {
        ProductType.INAPP -> BillingClient.SkuType.INAPP
        ProductType.SUBS -> BillingClient.SkuType.SUBS
        else -> null
    }
}
