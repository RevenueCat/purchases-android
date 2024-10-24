package com.revenuecat.purchases.deeplinks

import android.content.Intent
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog
import java.util.Collections
import java.util.HashSet

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
         * Deep link is recognized by RevenueCat's SDK but either it's not configured or it's not set to process
         * the deep link so it will be cached and processed after SDK has been configured correctly.
         */
        DEFERRED,

        /**
         * Deep link is not recognized by RevenueCat's SDK so it's ignored.
         */
        IGNORED,
    }

    companion object {
        private val cachedLinks = Collections.synchronizedSet(HashSet<DeepLinkParser.DeepLink>())

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
                    Result.DEFERRED
                }
            } else if (shouldCache) {
                cacheDeepLink(deepLink)
                return Result.DEFERRED
            } else {
                verboseLog("Deep link ignored because SDK is not configured and caching disabled: $deepLink")
                return Result.IGNORED
            }
        }

        internal fun forEachCachedLink(action: (DeepLinkParser.DeepLink) -> Boolean) {
            synchronized(cachedLinks) {
                val unprocessedDeepLinks = mutableSetOf<DeepLinkParser.DeepLink>()
                for (deepLink in cachedLinks) {
                    val hasBeenProcessed = action(deepLink)
                    if (!hasBeenProcessed) {
                        unprocessedDeepLinks.add(deepLink)
                    }
                }
                cachedLinks.clear()
                cachedLinks.addAll(unprocessedDeepLinks)
            }
        }

        internal fun clearCachedLinks() {
            synchronized(cachedLinks) {
                cachedLinks.clear()
            }
        }

        private fun cacheDeepLink(deepLink: DeepLinkParser.DeepLink) {
            verboseLog("Caching deep link for processing after SDK has been configured: $deepLink")
            synchronized(cachedLinks) {
                cachedLinks.add(deepLink)
            }
        }
    }
}
