package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.strings.BillingStrings
import java.util.ArrayList

fun PurchaseHistoryRecord.toHumanReadableDescription() =
    "skus: ${
        this.skus.joinToString(prefix = "[", postfix = "]")
    }, purchaseTime: ${this.purchaseTime}, purchaseToken: ${this.purchaseToken}"

val PurchaseHistoryRecord.listOfSkus: ArrayList<String>
    get() = this.skus

/**
 * **Important**: until multi-line subscriptions are released,
 * we assume that there will be only a single sku in the purchase.
 * https://android-developers.googleblog.com/2021/05/whats-new-in-google-play-2021.html
 */
val PurchaseHistoryRecord.firstSku: String
    get() = skus[0].also {
        if (skus.size > 0) {
            log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_PURCHASE_HISTORY_RECORD_MORE_THAN_ONE_SKU)
        }
    }
