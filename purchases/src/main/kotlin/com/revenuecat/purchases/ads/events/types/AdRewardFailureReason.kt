package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Reason a reward verification failed to verify.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmInline
public value class AdRewardFailureReason internal constructor(internal val value: String) {
    public companion object {
        public val TIMEOUT: AdRewardFailureReason = AdRewardFailureReason("timeout")
        public val NETWORK_ERROR: AdRewardFailureReason = AdRewardFailureReason("network_error")
        public val UNKNOWN: AdRewardFailureReason = AdRewardFailureReason("unknown")

        /**
         * The backend rejected the verification. [reason] carries the backend's machine-readable
         * code when provided, falling back to the generic `backend_error` wire value when absent.
         */
        public fun backendError(reason: String?): AdRewardFailureReason =
            AdRewardFailureReason(reason ?: "backend_error")
    }
}
