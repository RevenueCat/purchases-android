@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases

private const val TAG = "PurchasesAdMob"

/**
 * Executes [block] with the [Purchases] ad tracker if the SDK is configured.
 * If [Purchases] has not been configured yet, logs a warning and skips the block.
 *
 * This prevents crashes in ad callbacks when the developer has not yet called
 * [Purchases.configure].
 */
internal inline fun trackIfConfigured(block: Purchases.() -> Unit) {
    if (!Purchases.isConfigured) {
        Log.w(
            TAG,
            "Purchases is not configured. " +
                "Call Purchases.configure() before loading ads to enable RevenueCat ad tracking.",
        )
        return
    }
    Purchases.sharedInstance.block()
}
