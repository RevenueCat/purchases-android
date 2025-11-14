package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.EventsManager

/**
 * Common ad mediator names.
 */
@InternalRevenueCatAPI
object AdMediatorName {
    const val AD_MOB = "AdMob"
    const val APP_LOVIN = "AppLovin"
}

/**
 * Common ad revenue precision values.
 */
@InternalRevenueCatAPI
object AdRevenuePrecision {
    const val EXACT = "exact"
    const val PUBLISHER_DEFINED = "publisher_defined"
    const val ESTIMATED = "estimated"
    const val UNKNOWN = "unknown"
}

/**
 * Tracks ad-related events such as ad displays, opens, and revenue.
 */
@InternalRevenueCatAPI
class AdTracker internal constructor(
    private val eventsManager: EventsManager?,
) {

    /**
     * Tracks an ad displayed event.
     *
     * @param networkName The name of the ad network.
     * @param mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
     * @param placement The placement of the ad, if available.
     * @param adUnitId The ad unit ID.
     * @param impressionId The impression ID.
     */
    fun trackAdDisplayed(
        networkName: String,
        mediatorName: String,
        placement: String?,
        adUnitId: String,
        impressionId: String,
    ) {
        eventsManager?.track(
            event = AdEvent.Displayed(
                networkName = networkName,
                mediatorName = mediatorName,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = impressionId,
            ),
        )
    }

    /**
     * Tracks an ad opened event.
     *
     * @param networkName The name of the ad network.
     * @param mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
     * @param placement The placement of the ad, if available.
     * @param adUnitId The ad unit ID.
     * @param impressionId The impression ID.
     */
    fun trackAdOpened(
        networkName: String,
        mediatorName: String,
        placement: String?,
        adUnitId: String,
        impressionId: String,
    ) {
        eventsManager?.track(
            event = AdEvent.Open(
                networkName = networkName,
                mediatorName = mediatorName,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = impressionId,
            ),
        )
    }

    /**
     * Tracks an ad revenue event.
     *
     * @param networkName The name of the ad network.
     * @param mediatorName The name of the ad mediator. See [AdMediatorName] for common values.
     * @param placement The placement of the ad, if available.
     * @param adUnitId The ad unit ID.
     * @param impressionId The impression ID.
     * @param revenueMicros The revenue generated from the ad in micros.
     * @param currency The currency code for the revenue (e.g., "USD").
     * @param precision The precision of the revenue value. Should normally be one of [AdRevenuePrecision].
     */
    @Suppress("LongParameterList")
    fun trackAdRevenue(
        networkName: String,
        mediatorName: String,
        placement: String?,
        adUnitId: String,
        impressionId: String,
        revenueMicros: Long,
        currency: String,
        precision: String, // WIP Should normally be one of AdRevenuePrecision.
    ) {
        eventsManager?.track(
            event = AdEvent.Revenue(
                networkName = networkName,
                mediatorName = mediatorName,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = impressionId,
                revenueMicros = revenueMicros,
                currency = currency,
                precision = precision,
            ),
        )
    }
}
