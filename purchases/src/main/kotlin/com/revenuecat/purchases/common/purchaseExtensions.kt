package com.revenuecat.purchases.common

import com.android.billingclient.api.Purchase

internal fun Purchase.toHumanReadableDescription() =
    "productIds: ${
        this.products.joinToString(prefix = "[", postfix = "]")
    }, orderId: ${this.orderId}, purchaseToken: ${this.purchaseToken}"

/**
 * **Important**: until multi-line subscriptions are released,
 * we assume that there will be only a single sku in the purchase.
 * https://android-developers.googleblog.com/2021/05/whats-new-in-google-play-2021.html
 */
internal val Purchase.firstProductId: String
    get() = products[0]
