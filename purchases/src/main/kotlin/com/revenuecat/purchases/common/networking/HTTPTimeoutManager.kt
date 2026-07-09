//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import java.util.concurrent.ConcurrentHashMap

@OptIn(InternalRevenueCatAPI::class)
internal class HTTPTimeoutManager(
    private val appConfig: AppConfig,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {
    companion object {
        // Main-source request to an endpoint with no fallback-URL support
        const val MAIN_SOURCE_NO_FALLBACK_TIMEOUT_MS = 15000L // 15 seconds
        const val MAIN_SOURCE_NO_FALLBACK_REDUCED_TIMEOUT_MS = 5000L // 5 seconds after a recent timeout

        // Main-source request to an endpoint with fallback-URL support
        const val SUPPORTED_FALLBACK_TIMEOUT_MS = 5000L // 5 seconds
        const val REDUCED_TIMEOUT_MS = 2000L // 2 seconds after a recent timeout

        // Fallback-host request
        const val FALLBACK_HOST_TIMEOUT_MS = 30000L // 30 seconds

        // Proxied request
        const val DEFAULT_TIMEOUT_MS = 30000L // 30 seconds

        const val TIMEOUT_RESET_INTERVAL_MS = 600000L // 10 minutes
        const val TEST_DIVIDER = 10L // When running tests, we reduce timeouts
    }

    enum class RequestResult {
        /**
         * Request succeeded on the main source
         */
        SUCCESS_ON_MAIN_BACKEND,

        /**
         * Request timed out on the main source
         */
        MAIN_SOURCE_TIMED_OUT,

        /**
         * Any other result (fallback-host request, non-timeout errors, etc.)
         */
        OTHER_RESULT,
    }

    // The last time a timeout was recorded, keyed by resolved host string.
    private val lastTimeoutByHost = ConcurrentHashMap<String, Long>()

    /**
     * Calculates the timeout for a request attempt.
     * @param host The resolved host string of the attempt, used to look up the per-host fail-fast memory.
     * @param isFallback Whether this attempt targets a fallback host.
     * @param endpointSupportsFallbackURLs Whether the endpoint has fallback-URL support.
     * @param isProxied Whether a proxy URL is set.
     * @return The timeout in milliseconds.
     */
    fun getTimeoutForRequest(
        host: String?,
        isFallback: Boolean,
        endpointSupportsFallbackURLs: Boolean,
        isProxied: Boolean,
    ): Long {
        val timeout = when {
            // Fallback-host and proxied requests use a flat timeout and never consult the per-host memory.
            isFallback -> FALLBACK_HOST_TIMEOUT_MS
            isProxied -> DEFAULT_TIMEOUT_MS
            else -> {
                val sourceRecentlyTimedOut = host?.let { hasRecentTimeout(it) } ?: false
                when {
                    endpointSupportsFallbackURLs && sourceRecentlyTimedOut -> REDUCED_TIMEOUT_MS
                    endpointSupportsFallbackURLs -> SUPPORTED_FALLBACK_TIMEOUT_MS
                    sourceRecentlyTimedOut -> MAIN_SOURCE_NO_FALLBACK_REDUCED_TIMEOUT_MS
                    else -> MAIN_SOURCE_NO_FALLBACK_TIMEOUT_MS
                }
            }
        }

        return if (appConfig.runningTests) {
            timeout / TEST_DIVIDER
        } else {
            timeout
        }
    }

    /**
     * Records the result of a request attempt and updates the per-host memory accordingly.
     * @param host The resolved host string of the attempt.
     * @param result The result of the request.
     */
    fun recordRequestResult(host: String?, result: RequestResult) {
        if (host == null) return

        when (result) {
            RequestResult.SUCCESS_ON_MAIN_BACKEND -> {
                lastTimeoutByHost.remove(host)
            }
            RequestResult.MAIN_SOURCE_TIMED_OUT -> {
                lastTimeoutByHost[host] = dateProvider.now.time
            }
            RequestResult.OTHER_RESULT -> {
                // No state update needed
            }
        }
    }

    /**
     * Whether [host] has a non-expired timeout entry. Prunes the entry if it has expired.
     */
    private fun hasRecentTimeout(host: String): Boolean {
        val lastTimeout = lastTimeoutByHost[host] ?: return false
        val elapsed = dateProvider.now.time - lastTimeout
        if (elapsed >= TIMEOUT_RESET_INTERVAL_MS) {
            lastTimeoutByHost.remove(host)
            return false
        }
        return true
    }
}
