package com.revenuecat.apitester.kotlin

import android.content.Intent
import com.revenuecat.purchases.deeplinks.DeepLinkHandler

@Suppress("unused", "UNUSED_VARIABLE")
private class DeepLinkHandlerAPI {
    fun check(intent: Intent, shouldCache: Boolean) {
        val handleResult: DeepLinkHandler.HandleResult = DeepLinkHandler.handleDeepLink(intent, shouldCache)
    }

    fun checkHandleResult(handleResult: DeepLinkHandler.HandleResult): Boolean {
        when (handleResult) {
            DeepLinkHandler.HandleResult.HANDLED,
            DeepLinkHandler.HandleResult.DEFERRED_TO_SDK_CONFIGURATION,
            DeepLinkHandler.HandleResult.IGNORED -> {
                return true
            }
        }
    }
}
