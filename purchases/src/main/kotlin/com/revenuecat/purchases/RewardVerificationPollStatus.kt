package com.revenuecat.purchases

/**
 * Result of a single reward verification status request.
 */
@InternalRevenueCatAPI
public sealed interface RewardVerificationPollStatus {
    /**
     * Verified by the backend with the associated reward payload.
     */
    public data class Verified(val reward: VerifiedReward) : RewardVerificationPollStatus

    /**
     * Verification has started but has not yet reached a terminal state.
     */
    public object PENDING : RewardVerificationPollStatus

    /**
     * Verification reached a terminal failure state.
     *
     * @param failureReason Machine-readable reason code from the backend, when provided.
     * @param message Human-readable, actionable explanation from the backend, when provided.
     */
    public data class Failed(
        val failureReason: String? = null,
        val message: String? = null,
    ) : RewardVerificationPollStatus

    /**
     * The backend returned a status value that is not recognized by this SDK version.
     */
    public object UNKNOWN : RewardVerificationPollStatus
}
