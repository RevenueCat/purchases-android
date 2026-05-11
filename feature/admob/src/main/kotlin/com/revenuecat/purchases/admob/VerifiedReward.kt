package com.revenuecat.purchases.admob

/**
 * Reward payload delivered when server-side reward verification succeeds.
 */
public sealed interface VerifiedReward {

    /**
     * A virtual-currency reward with a code and amount.
     */
    public class VirtualCurrency(
        public val code: String,
        public val amount: Int,
    ) : VerifiedReward

    /**
     * Verification succeeded but no reward was granted.
     */
    public object NoReward : VerifiedReward

    /**
     * Verification succeeded but the reward type is not modeled by this SDK version.
     */
    public object UnsupportedReward : VerifiedReward
}
