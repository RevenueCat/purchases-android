package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.RCProductType

fun String?.toProductType(): RCProductType {
    return when (this) {
        BillingClient.ProductType.INAPP -> RCProductType.INAPP
        BillingClient.ProductType.SUBS -> RCProductType.SUBS
        else -> RCProductType.UNKNOWN
    }
}

fun RCProductType.toGoogleProductType(): String? {
    return when (this) {
        RCProductType.INAPP -> BillingClient.ProductType.INAPP
        RCProductType.SUBS -> BillingClient.ProductType.SUBS
        else -> null
    }
}
