//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import java.util.concurrent.atomic.AtomicLong

internal class HTTPTimeoutManager(
    private val appConfig: AppConfig,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {
    companion object {
        const val SUPPORTED_FALLBACK_TIMEOUT_MS = 5000L // 5 seconds for requests with fallback support
        const val REDUCED_TIMEOUT_MS = 2000L // 2 seconds for requests with fallback support after timeout
        const val DEFAULT_TIMEOUT_MS = 30000L // 30 seconds for requests without fallback support and fallback requests
        const val TIMEOUT_RESET_INTERVAL_MS = 600000L // 10 minutes
        const val TEST_DIVIDER = 10L // When running tests, we reduce timeouts
    }

    enum class RequestResult {
        /**
         * Request succeeded on the main backend
         */
        SUCCESS_ON_MAIN_BACKEND,

        /**
         * Request timed out on the main backend in fallback supported endpoint
         */
        TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT,

        /**
         * Any other result (non-main backend, non-timeout errors, etc.)
         */
        OTHER_RESULT,
    }

    private val lastTimeoutRequestTime = AtomicLong(0L)

    /**
     * Calculates the timeout for a request based on the endpoint and whether it's a fallback call.
     * @param endpoint The endpoint being requested
     * @param isFallback Whether this is a fallback request
     * @return The timeout in milliseconds
     */
    public fun getTimeoutForRequest(endpoint: Endpoint, isFallback: Boolean): Long {
        // Check if reset is needed (10 minutes elapsed)
        if (shouldResetTimeout()) {
            resetTimeout()
        }

        val timeout = when {
            isFallback -> DEFAULT_TIMEOUT_MS
            !endpoint.supportsFallbackBaseURLs -> DEFAULT_TIMEOUT_MS
            lastTimeoutRequestTime.get() > 0L -> REDUCED_TIMEOUT_MS
            else -> SUPPORTED_FALLBACK_TIMEOUT_MS
        }

        return if (appConfig.runningTests) {
            timeout / TEST_DIVIDER
        } else {
            timeout
        }
    }

    /**
     * Records the result of a request and updates internal state accordingly.
     * @param result The result of the request
     */
    public fun recordRequestResult(result: RequestResult) {
        when (result) {
            RequestResult.SUCCESS_ON_MAIN_BACKEND -> {
                resetTimeout()
            }
            RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT -> {
                lastTimeoutRequestTime.set(dateProvider.now.time)
            }
            RequestResult.OTHER_RESULT -> {
                // No state update needed
            }
        }
    }

    /**
     * Checks if a reset is needed based on the 10-minute interval.
     * Returns true if 10 minutes have elapsed since the last timeout request.
     */
    private fun shouldResetTimeout(): Boolean {
        val lastTimeoutTime = lastTimeoutRequestTime.get()
        if (lastTimeoutTime == 0L) {
            return false
        }
        val now = dateProvider.now.time
        return (now - lastTimeoutTime) >= TIMEOUT_RESET_INTERVAL_MS
    }

    /**
     * Resets the timeout state (clears previous timeout flag).
     */
    private fun resetTimeout() {
        lastTimeoutRequestTime.set(0L)
    }
}
