package com.revenuecat.purchases.admob

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import dev.drewhamilton.poko.Poko

/**
 * Reward payload delivered when server-side reward verification succeeds.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public interface VerifiedReward {

    /**
     * A virtual-currency reward with a code and amount.
     */
    @Poko
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
