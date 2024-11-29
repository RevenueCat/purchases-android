package com.revenuecat.purchases

import android.content.Intent

@ExperimentalPreviewRevenueCatPurchasesAPI
val Intent.asWebPurchaseRedemption: WebPurchaseRedemption?
    @JvmSynthetic
    get() = Purchases.parseAsWebPurchaseRedemption(this)
