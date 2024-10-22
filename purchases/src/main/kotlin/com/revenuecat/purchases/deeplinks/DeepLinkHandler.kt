package com.revenuecat.purchases.deeplinks

import android.content.Intent
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog

/**
 * Allows to pass deep links to the RevenueCat SDK for processing.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
class DeepLinkHandler {
    /**
     * Represents whether the deep link will be handled by the SDK or not.
     */
    enum class Result {
        /**
         * Deep link is recognized by RevenueCat's SDK and will be handled.
         */
        HANDLED,

        /**
         * Deep link is recognized by RevenueCat's SDK but it's not configured so it will be cached
         * and processed after SDK has been configured.
         */
        DEFERRED_TO_SDK_CONFIGURATION,

        /**
         * Deep link is not recognized by RevenueCat's SDK so it's ignored.
         */
        IGNORED,
    }

    companion object {
        @get:Synchronized
        internal val cachedLinks = mutableSetOf<DeepLinkParser.DeepLink>()

        /**
         * Allows the RevenueCat SDK to handle the activity intent to process relevant deep links.
         * @param intent The intent to process deep links for. Usually obtained from the Activity.
         * @param shouldCache Whether the deep link should be cached for later processing if the SDK is not configured.
         */
        @Suppress("ReturnCount")
        @JvmStatic
        fun handleDeepLink(intent: Intent, shouldCache: Boolean = true): Result {
            val deepLink = intent.data?.let {
                DeepLinkParser().parseDeepLink(it)
            } ?: return Result.IGNORED
            if (Purchases.isConfigured) {
                debugLog("Handling deep link: $deepLink")
                val handleResult = Purchases.sharedInstance.handleDeepLink(deepLink)
                return if (handleResult) {
                    Result.HANDLED
                } else {
                    Result.IGNORED
                }
            } else if (shouldCache) {
                cacheDeepLink(deepLink)
                return Result.DEFERRED_TO_SDK_CONFIGURATION
            } else {
                verboseLog("Deep link ignored because SDK is not configured and caching disabled: $deepLink")
                return Result.IGNORED
            }
        }

        private fun cacheDeepLink(deepLink: DeepLinkParser.DeepLink) {
            verboseLog("Caching deep link for processing after SDK has been configured: $deepLink")
            cachedLinks.add(deepLink)
        }
    }
}
