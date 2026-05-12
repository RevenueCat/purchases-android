package com.revenuecat.purchases

/**
 * Status returned by RevenueCat for an AdMob reward verification request.
 */
@InternalRevenueCatAPI
public enum class AdMobRewardVerificationStatus {
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
