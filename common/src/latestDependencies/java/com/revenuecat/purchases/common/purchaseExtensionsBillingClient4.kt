package com.revenuecat.purchases.common

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.strings.BillingStrings
import java.util.ArrayList

fun Purchase.toHumanReadableDescription() =
    "skus: ${
        this.skus.joinToString(prefix = "[", postfix = "]")
    }, orderId: ${this.orderId}, purchaseToken: ${this.purchaseToken}"

/**
 * **Important**: until multi-line subscriptions are released,
 * we assume that there will be only a single sku in the purchase.
 * https://android-developers.googleblog.com/2021/05/whats-new-in-google-play-2021.html
 */
val Purchase.firstSku: String
    get() = skus[0].also {
        if (skus.size > 1) {
            log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_PURCHASE_MORE_THAN_ONE_SKU)
        }
    }

val Purchase.listOfSkus: ArrayList<String>
    get() = this.skus
