package com.revenuecat.purchases

import android.content.Intent

@JvmSynthetic
fun Intent.asWebPurchaseRedemption(): WebPurchaseRedemption? {
    return Purchases.parseAsWebPurchaseRedemption(this)
}
