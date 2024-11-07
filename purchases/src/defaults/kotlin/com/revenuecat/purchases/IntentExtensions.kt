package com.revenuecat.purchases

import android.content.Intent

@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
fun Intent.parseAsWebPurchaseRedemption(): WebPurchaseRedemption? {
    return Purchases.parseAsWebPurchaseRedemption(this)
}
