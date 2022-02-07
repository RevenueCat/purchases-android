package com.revenuecat.purchases.utils

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord

fun stubGooglePurchase(
    productIds: List<String> = listOf("com.revenuecat.lifetime"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijipehfnbbnldmai.AO-J1OxqriTepvB7suzlIhxqPIveA0IHtX9amMedK0KK9CsO0S3Zk5H6gdwvV" +
        "7HzZIJeTzqkY4okyVk8XKTmK1WZKAKSNTKop4dgwSmFnLWsCxYbahUmADg",
    signature: String = "signature${System.currentTimeMillis()}",
    purchaseState: Int = Purchase.PurchaseState.PURCHASED,
    acknowledged: Boolean = true,
    orderId: String = "GPA.3372-4150-8203-17209"
): Purchase = Purchase(
    """
    {
        "orderId": "$orderId",
        "packageName":"com.revenuecat.purchases_sample",
        "productId":${productIds[0]},
        "purchaseTime":$purchaseTime,
        "purchaseState":${if (purchaseState == 2) 4 else 1},
        "purchaseToken":"$purchaseToken",
        "acknowledged":$acknowledged
    }
        """.trimIndent(), signature
)

fun stubPurchaseHistoryRecord(
    productIds: List<String> = listOf("monthly_intro_pricing_one_week"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijkcopgbomfinlko.AO-J1OxJixLsieYN08n9hV4qBsvqvQo6wXesyAClWs-t7KnYLCm3-" +
        "q6z8adcZnenbzqMHuMIqZ9kQ4KebT_Bge6KfZUhBt-0N0U0s71AEwFpzT7hrtErzdg",
    signature: String = "signature${System.currentTimeMillis()}"
): PurchaseHistoryRecord = PurchaseHistoryRecord("""
            {
                "productId": ${productIds[0]},
                "purchaseTime": $purchaseTime,
                "purchaseToken": "$purchaseToken"
            }
        """.trimIndent(), signature)
