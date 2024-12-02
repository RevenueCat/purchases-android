package com.revenuecat.purchases

import android.content.Intent

@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
fun Intent.asWebPurchaseRedemption(): WebPurchaseRedemption? {
    return Purchases.parseAsWebPurchaseRedemption(this)
}
