package com.revenuecat.purchases.admob

/**
 * Result delivered to the app after reward verification polling for a rewarded or rewarded interstitial ad.
 */
public class RewardVerificationResult private constructor(
    private val storage: Storage,
) {

    private sealed interface Storage {
        class Verified(val reward: VerifiedReward) : Storage
        object Failed : Storage
    }

    /**
     * Non-null when verification succeeded.
     */
    public val verifiedReward: VerifiedReward?
        get() = (this.storage as? Storage.Verified)?.reward

    /**
     * True when verification did not complete successfully.
     */
    public val isFailed: Boolean
        get() = this.storage is Storage.Failed

    public companion object {

        /**
         * Server verification succeeded for this ad transaction.
         */
        @JvmStatic
        public fun verified(reward: VerifiedReward): RewardVerificationResult {
            return RewardVerificationResult(Storage.Verified(reward))
        }

        /**
         * Verification did not complete successfully (rejected, timeout, network, etc.).
         */
        @JvmField
        public val failed: RewardVerificationResult = RewardVerificationResult(Storage.Failed)
    }
}
