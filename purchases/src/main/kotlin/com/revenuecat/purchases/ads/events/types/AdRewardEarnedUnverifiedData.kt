package com.revenuecat.purchases.ads.events.types

import dev.drewhamilton.poko.Poko

/**
 * Data for the moment an ad SDK reports that the user earned a reward, before backend verification.
 *
 * @property networkName The name of the ad network, or null if unknown.
 * @property mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
 * @property adFormat The format of the ad. See [AdFormat] for common values.
 * @property placement The placement of the ad, if available.
 * @property adUnitId The ad unit ID.
 * @property impressionId The impression ID.
 * @property rewardVerificationEnabled Whether server-side reward verification is enabled for this ad.
 */
@Poko
public class AdRewardEarnedUnverifiedData(
    public val networkName: String?,
    public val mediatorName: AdMediatorName,
    public val adFormat: AdFormat,
    public val placement: String?,
    public val adUnitId: String,
    public val impressionId: String,
    public val rewardVerificationEnabled: Boolean = false,
)
