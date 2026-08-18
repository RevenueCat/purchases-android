package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import dev.drewhamilton.poko.Poko

/**
 * Ties a loaded rewarded ad to its server-side reward verification.
 *
 * Produced by [Purchases.generateRewardVerificationToken]. Forward [customData] and [appUserID] to your
 * ad network's server-side verification options, and keep [clientTransactionId] to correlate the reward
 * callback with [Purchases.pollRewardVerification].
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
public class RewardVerificationToken(
    /** Set as the ad network's server-side verification custom data. */
    public val customData: String,
    /** Correlates the ad with its verification. */
    public val clientTransactionId: String,
    /** The app user the reward is attributed to; set as the ad network's SSV user identifier. */
    public val appUserID: String,
)
