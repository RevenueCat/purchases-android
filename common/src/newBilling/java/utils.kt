package com.revenuecat.purchases.common

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord

fun Purchase.toHumanReadableDescription() =
    "skus: ${
        this.skus.joinToString(prefix = "[", postfix = "]")
    }, orderId: ${this.orderId}, purchaseToken: ${this.purchaseToken}"

fun PurchaseHistoryRecord.toHumanReadableDescription() =
    "skus: ${
        this.skus.joinToString(prefix = "[", postfix = "]")
    }, purchaseTime: ${this.purchaseTime}, purchaseToken: ${this.purchaseToken}"
