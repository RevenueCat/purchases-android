package com.revenuecat.purchases

/**
 * Ties a loaded rewarded ad to its server-side reward verification.
 *
 * Produced by [Purchases.generateRewardVerificationToken]. Forward [customData] and [appUserID] to your
 * ad network's server-side verification options, and keep [clientTransactionId] to correlate the reward
 * callback with [Purchases.pollRewardVerification].
 */
@InternalRevenueCatAPI
public data class RewardVerificationToken(
    /** Set as the ad network's server-side verification custom data. */
    val customData: String,
    /** Correlates the ad with its verification. */
    val clientTransactionId: String,
    /** The app user the reward is attributed to; set as the ad network's SSV user identifier. */
    val appUserID: String,
)
