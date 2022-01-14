package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import java.util.ArrayList

fun PurchaseHistoryRecord.toHumanReadableDescription() =
    "skus: ${
        this.skus.joinToString(prefix = "[", postfix = "]")
    }, purchaseTime: ${this.purchaseTime}, purchaseToken: ${this.purchaseToken}"

val PurchaseHistoryRecord.listOfSkus: ArrayList<String>
    get() = this.skus
