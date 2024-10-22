package com.revenuecat.apitester.kotlin

import android.content.Intent
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.deeplinks.DeepLinkHandler

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class DeepLinkHandlerAPI {
    fun check(intent: Intent, shouldCache: Boolean) {
        val result: DeepLinkHandler.Result = DeepLinkHandler.handleDeepLink(intent, shouldCache)
    }

    fun checkHandleResult(result: DeepLinkHandler.Result): Boolean {
        when (result) {
            DeepLinkHandler.Result.HANDLED,
            DeepLinkHandler.Result.DEFERRED,
            DeepLinkHandler.Result.IGNORED,
            -> {
                return true
            }
        }
    }
}
