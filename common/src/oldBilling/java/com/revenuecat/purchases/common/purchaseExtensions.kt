package com.revenuecat.purchases.common

import com.android.billingclient.api.Purchase

fun Purchase.toHumanReadableDescription() =
    "sku: ${
        this.sku
    }, orderId: ${this.orderId}, purchaseToken: ${this.purchaseToken}"

val Purchase.skus: List<String>
    get() = listOf(this.sku)
