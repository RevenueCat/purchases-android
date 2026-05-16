package com.revenuecat.purchases

/**
 * Reward payload carried by a verified reward-verification result.
 */
@InternalRevenueCatAPI
public sealed interface VerifiedReward {
    /**
     * A virtual-currency reward.
     */
    public data class VirtualCurrency(
        val code: String,
        val amount: Int,
    ) : VerifiedReward

    /**
     * Verified with no reward payload.
     */
    public object NoReward : VerifiedReward

    /**
     * Verified with a reward payload not modeled by this SDK version.
     */
    public object UnsupportedReward : VerifiedReward
}
