package com.revenuecat.purchases.ads.events.types

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import dev.drewhamilton.poko.Poko

/**
 * Data for tracking ad revenue events.
 *
 * @property networkName The name of the ad network, or null if unknown.
 * @property mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
 * @property adFormat The format of the ad. See [AdFormat] for common values.
 * @property placement The placement of the ad, if available.
 * @property adUnitId The ad unit ID.
 * @property impressionId The impression ID.
 * @property revenueMicros The revenue generated from the ad in micros.
 * @property currency The currency code for the revenue (e.g., "USD").
 * @property precision The precision of the revenue value. See [AdRevenuePrecision].
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
public class AdRevenueData(
    public val networkName: String?,
    public val mediatorName: AdMediatorName,
    public val adFormat: AdFormat,
    public val placement: String?,
    public val adUnitId: String,
    public val impressionId: String,
    public val revenueMicros: Long,
    public val currency: String,
    public val precision: AdRevenuePrecision,
)
