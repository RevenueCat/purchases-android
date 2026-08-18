package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import dev.drewhamilton.poko.Poko
import java.util.Date

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
     * A temporary entitlement grant with an identifier and expiration.
     */
    @Poko
    public class Entitlement(
        public val identifier: String,
        public val expiresAt: Date,
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
