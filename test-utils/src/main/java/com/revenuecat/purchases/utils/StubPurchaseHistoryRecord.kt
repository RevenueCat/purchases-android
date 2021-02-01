package com.revenuecat.purchases.utils

import com.android.billingclient.api.PurchaseHistoryRecord

fun stubPurchaseHistoryRecord(
    productId: String = "monthly_intro_pricing_one_week",
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijkcopgbomfinlko.AO-J1OxJixLsieYN08n9hV4qBsvqvQo6wXesyAClWs-t7KnYLCm3-" +
        "q6z8adcZnenbzqMHuMIqZ9kQ4KebT_Bge6KfZUhBt-0N0U0s71AEwFpzT7hrtErzdg",
    signature: String = "signature${System.currentTimeMillis()}"
): PurchaseHistoryRecord = PurchaseHistoryRecord("""
            {
                "productId": "$productId",
                "purchaseTime": $purchaseTime,
                "purchaseToken": "$purchaseToken"
            }
        """.trimIndent(), signature)
