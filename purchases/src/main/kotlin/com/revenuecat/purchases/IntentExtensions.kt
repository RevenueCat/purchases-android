package com.revenuecat.purchases

import android.content.Intent

@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
fun Intent.parseAsDeepLink(): Purchases.DeepLink? {
    return Purchases.parseAsDeepLink(this)
}
