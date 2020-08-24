package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingClient

enum class PurchaseType {
    SUBS, INAPP, UNKNOWN;

    companion object {
        fun fromSKUType(@BillingClient.SkuType skuType: String?): PurchaseType {
            return when (skuType) {
                BillingClient.SkuType.INAPP -> INAPP
                BillingClient.SkuType.SUBS -> SUBS
                else -> UNKNOWN
            }
        }
    }
}

fun PurchaseType.toSKUType(): String? {
    return when (this) {
        PurchaseType.INAPP -> BillingClient.SkuType.INAPP
        PurchaseType.SUBS -> BillingClient.SkuType.SUBS
        PurchaseType.UNKNOWN -> null
    }
}
