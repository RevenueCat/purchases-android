package com.revenuecat.purchases

/**
 * Result of a single reward verification status request.
 */
@InternalRevenueCatAPI
public sealed interface RewardVerificationResult {
    /**
     * Verified by the backend with the associated reward payload.
     */
    public data class Verified(val reward: VerifiedReward) : RewardVerificationResult

    /**
     * Verification has started but has not yet reached a terminal state.
     */
    public object PENDING : RewardVerificationResult

    /**
     * Verification reached a terminal failure state.
     */
    public object FAILED : RewardVerificationResult

    /**
     * The backend returned a status value that is not recognized by this SDK version.
     */
    public object UNKNOWN : RewardVerificationResult
}
