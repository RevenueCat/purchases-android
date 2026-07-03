package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Result delivered to the app after reward verification polling for a rewarded or rewarded interstitial ad.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public class RewardVerificationResult private constructor(
    private val storage: Storage,
) {

    private sealed interface Storage {
        class Verified(val reward: VerifiedReward, val moreRewards: List<VerifiedReward>) : Storage
        object Failed : Storage
    }

    /**
     * The primary reward when verification succeeded, null otherwise.
     */
    public val verifiedReward: VerifiedReward?
        get() = (this.storage as? Storage.Verified)?.reward

    /**
     * Additional rewards granted alongside [verifiedReward]; does not repeat it. Empty when verification failed.
     */
    public val moreRewards: List<VerifiedReward>
        get() = (this.storage as? Storage.Verified)?.moreRewards ?: emptyList()

    /**
     * True when verification did not complete successfully.
     */
    public val failed: Boolean
        get() = this.storage is Storage.Failed

    public companion object {

        /**
         * Server verification succeeded for this ad transaction.
         *
         * @param moreRewards Additional rewards granted alongside [reward]; should not repeat it.
         */
        @JvmStatic
        @JvmOverloads
        @InternalRevenueCatAPI
        public fun verified(
            reward: VerifiedReward,
            moreRewards: List<VerifiedReward> = emptyList(),
        ): RewardVerificationResult {
            return RewardVerificationResult(Storage.Verified(reward, moreRewards))
        }

        /**
         * Verification did not complete successfully (rejected, timeout, network, etc.).
         */
        @JvmField
        public val failed: RewardVerificationResult = RewardVerificationResult(Storage.Failed)
    }
}
