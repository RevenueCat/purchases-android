package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.ProductType

fun @receiver:BillingClient.ProductType String?.toRevenueCatProductType(): ProductType {
    return when (this) {
        BillingClient.ProductType.INAPP -> ProductType.INAPP
        BillingClient.ProductType.SUBS -> ProductType.SUBS
        else -> ProductType.UNKNOWN
    }
}

fun ProductType.toGoogleProductType(): String? {
    return when (this) {
        ProductType.INAPP -> BillingClient.ProductType.INAPP
        ProductType.SUBS -> BillingClient.ProductType.SUBS
        else -> null
    }
}
