package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.ProductType

fun @receiver:BillingClient.SkuType String?.toRevenueCatProductType(): ProductType {
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

// The key associated with the product ID
// the value associated with this key is what we query BillingClient on
// on BC4, the value is the sku. on BC5, it's the subscription ID
const val SUBSCRIPTION_ID_BACKEND_KEY = "platform_product_identifier"
