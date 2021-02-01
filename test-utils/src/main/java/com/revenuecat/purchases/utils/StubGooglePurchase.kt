package com.revenuecat.purchases.utils

import com.android.billingclient.api.Purchase

fun stubGooglePurchase(
    productId: String = "com.revenuecat.lifetime",
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
        "productId":"$productId",
        "purchaseTime":$purchaseTime,
        "purchaseState":$purchaseState,
        "purchaseToken":"$purchaseToken",
        "acknowledged":$acknowledged
    }
        """.trimIndent(), signature
)
