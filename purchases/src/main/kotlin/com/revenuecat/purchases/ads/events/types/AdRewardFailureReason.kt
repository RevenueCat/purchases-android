package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Reason a reward verification failed to verify.
 */
@InternalRevenueCatAPI
public abstract class AdRewardFailureReason internal constructor(internal val value: String) {

    public object Timeout : AdRewardFailureReason("timeout")

    public object NetworkError : AdRewardFailureReason("network_error")

    public object Cancelled : AdRewardFailureReason("cancelled")

    public object Unknown : AdRewardFailureReason("unknown")

    /**
     * The backend rejected the verification. [reason] carries the backend's machine-readable
     * code when provided, falling back to the generic `backend_error` wire value when absent.
     */
    public data class BackendError(val reason: String?) : AdRewardFailureReason(reason ?: "backend_error")
}
