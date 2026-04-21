package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import dev.drewhamilton.poko.Poko

/**
 * Data for tracking when an ad has been loaded.
 *
 * @property networkName The name of the ad network, or null if unknown.
 * @property mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
 * @property adFormat The format of the ad. See [AdFormat] for common values.
 * @property placement The placement of the ad, if available.
 * @property adUnitId The ad unit ID.
 * @property impressionId The impression ID.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
public class AdLoadedData(
    public val networkName: String?,
    public val mediatorName: AdMediatorName,
    public val adFormat: AdFormat,
    public val placement: String?,
    public val adUnitId: String,
    public val impressionId: String,
)
