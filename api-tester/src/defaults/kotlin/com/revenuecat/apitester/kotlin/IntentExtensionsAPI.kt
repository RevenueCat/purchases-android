package com.revenuecat.apitester.kotlin

import android.content.Intent
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.parseAsDeepLink

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class IntentExtensionsAPI {
    fun check(intent: Intent) {
        val deepLink: Purchases.DeepLink? = intent.parseAsDeepLink()
    }
}
