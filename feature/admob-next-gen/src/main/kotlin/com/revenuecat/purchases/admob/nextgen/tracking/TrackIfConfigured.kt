package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.Logger

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal inline fun trackIfConfigured(block: Purchases.() -> Unit) {
    if (!Purchases.isConfigured) {
        Logger.w(
            "Purchases is not configured. " +
                "Call Purchases.configure() before loading ads to enable RevenueCat ad tracking.",
        )
        return
    }
    Purchases.sharedInstance.block()
}
