package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.BillingStrings

/**
 * **Important**: until multi-line subscriptions are released,
 * we assume that there will be only a single sku in the purchase.
 * https://android-developers.googleblog.com/2021/05/whats-new-in-google-play-2021.html
 */
val PurchaseHistoryRecord.sku: String
    get() = skus[0].also {
        if (skus.size > 1) {
            log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_PURCHASE_HISTORY_RECORD_MORE_THAN_ONE_SKU)
        }
    }

/**
 * **Important**: until multi-line subscriptions are released,
 * we assume that there will be only a single sku in the purchase.
 * https://android-developers.googleblog.com/2021/05/whats-new-in-google-play-2021.html
 */
val Purchase.sku: String
    get() = skus[0].also {
        if (skus.size > 1) {
            log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_PURCHASE_MORE_THAN_ONE_SKU)
        }
    }
