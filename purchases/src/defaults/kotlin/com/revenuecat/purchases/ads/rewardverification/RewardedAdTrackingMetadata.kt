package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import dev.drewhamilton.poko.Poko

/**
 * Identifies the ad a reward-verification poll belongs to, so [Purchases.pollRewardVerification] can track
 * reward events for it. Omit this parameter (or pass null) to poll without tracking.
 *
 * @property networkName The name of the ad network, or null if unknown.
 * @property mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
 * @property adFormat The format of the ad. See [AdFormat] for common values.
 * @property placement The placement of the ad, if available.
 * @property adUnitId The ad unit ID.
 * @property impressionId The impression ID.
 */
@Poko
public class RewardedAdTrackingMetadata(
    public val networkName: String?,
    public val mediatorName: AdMediatorName,
    public val adFormat: AdFormat,
    public val placement: String?,
    public val adUnitId: String,
    public val impressionId: String,
)
