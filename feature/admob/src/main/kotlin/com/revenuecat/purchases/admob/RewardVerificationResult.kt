package com.revenuecat.purchases.admob

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Result delivered to the app after reward verification polling for a rewarded or rewarded interstitial ad.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
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
    public val failed: Boolean
        get() = this.storage is Storage.Failed

    internal companion object {

        /**
         * Server verification succeeded for this ad transaction.
         */
        @JvmStatic
        internal fun verified(reward: VerifiedReward): RewardVerificationResult {
            return RewardVerificationResult(Storage.Verified(reward))
        }

        /**
         * Verification did not complete successfully (rejected, timeout, network, etc.).
         */
        @JvmField
        internal val failed: RewardVerificationResult = RewardVerificationResult(Storage.Failed)
    }
}
