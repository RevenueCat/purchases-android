package com.revenuecat.purchases.common

import com.android.billingclient.api.Purchase

fun Purchase.toHumanReadableDescription() =
    "sku: ${
        this.sku
    }, orderId: ${this.orderId}, purchaseToken: ${this.purchaseToken}"

val Purchase.firstSku: String
    get() = sku

val Purchase.listOfSkus: List<String>
    get() = listOf(this.sku)
