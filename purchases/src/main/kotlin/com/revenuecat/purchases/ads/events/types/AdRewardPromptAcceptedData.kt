package com.revenuecat.purchases.ads.events.types

import dev.drewhamilton.poko.Poko

/**
 * Data for tracking when the app user accepts a prompt to watch a rewarded ad.
 *
 * @property mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
 * @property placement The placement of the prompt, if available.
 * @property adUnitId The ad unit ID of the rewarded ad.
 */
@Poko
public class AdRewardPromptAcceptedData(
    public val mediatorName: AdMediatorName,
    public val placement: String?,
    public val adUnitId: String,
)
