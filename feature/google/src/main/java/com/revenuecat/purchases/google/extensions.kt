package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord

val PurchaseHistoryRecord.sku: String
    get() = skus[0]

val Purchase.sku: String
    get() = skus[0]
