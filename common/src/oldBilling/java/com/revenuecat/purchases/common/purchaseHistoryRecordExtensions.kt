package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord

fun PurchaseHistoryRecord.toHumanReadableDescription() =
    "sku: ${
        this.sku
    }, purchaseTime: ${this.purchaseTime}, purchaseToken: ${this.purchaseToken}"

val PurchaseHistoryRecord.listOfSkus: List<String>
    get() = listOf(this.sku)
