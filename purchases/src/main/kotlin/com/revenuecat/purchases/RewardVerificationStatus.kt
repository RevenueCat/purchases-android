package com.revenuecat.purchases

/**
 * Status returned by RevenueCat for a reward verification request.
 */
@InternalRevenueCatAPI
public enum class RewardVerificationStatus {
    /**
     * Verification has started but has not yet reached a terminal state.
     */
    PENDING,

    /**
     * Verification succeeded and the reward is considered valid.
     */
    VERIFIED,

    /**
     * Verification reached a terminal failure state.
     */
    FAILED,

    /**
     * The backend returned a status value that is not recognized by this SDK version.
     */
    UNKNOWN,
}
