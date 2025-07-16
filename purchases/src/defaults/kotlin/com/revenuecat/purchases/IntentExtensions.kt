package com.revenuecat.purchases

import android.content.Intent

@JvmSynthetic
public fun Intent.asWebPurchaseRedemption(): WebPurchaseRedemption? {
    return Purchases.parseAsWebPurchaseRedemption(this)
}
