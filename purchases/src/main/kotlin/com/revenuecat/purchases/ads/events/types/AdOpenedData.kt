package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalRevenueCatAPI
import dev.drewhamilton.poko.Poko

/**
 * Data for tracking when a user opens/clicks on an ad.
 *
 * @property networkName The name of the ad network.
 * @property mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
 * @property placement The placement of the ad, if available.
 * @property adUnitId The ad unit ID.
 * @property impressionId The impression ID.
 */
@ExperimentalRevenueCatAPI
@Poko
class AdOpenedData(
    val networkName: String,
    val mediatorName: AdMediatorName,
    val placement: String?,
    val adUnitId: String,
    val impressionId: String,
)
