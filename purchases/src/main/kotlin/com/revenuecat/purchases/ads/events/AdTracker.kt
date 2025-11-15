package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.EventsManager

/**
 * Common ad mediator names.
 */
@InternalRevenueCatAPI
@JvmInline
value class AdMediatorName internal constructor(internal val value: String) {
    companion object {
        val AD_MOB = AdMediatorName("AdMob")
        val APP_LOVIN = AdMediatorName("AppLovin")

        fun fromString(value: String): AdMediatorName {
            return when (value.trim()) {
                "AdMob" -> AD_MOB
                "AppLovin" -> APP_LOVIN
                else -> AdMediatorName(value)
            }
        }
    }
}

/**
 * Common ad revenue precision values.
 */
@InternalRevenueCatAPI
@JvmInline
value class AdRevenuePrecision internal constructor(internal val value: String) {
    companion object {
        val EXACT = AdRevenuePrecision("exact")
        val PUBLISHER_DEFINED = AdRevenuePrecision("publisher_defined")
        val ESTIMATED = AdRevenuePrecision("estimated")
        val UNKNOWN = AdRevenuePrecision("unknown")

        fun fromString(value: String): AdRevenuePrecision {
            return when (value.lowercase().trim()) {
                "exact" -> EXACT
                "publisher_defined" -> PUBLISHER_DEFINED
                "estimated" -> ESTIMATED
                "unknown" -> UNKNOWN
                else -> AdRevenuePrecision(value)
            }
        }
    }
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
        mediatorName: AdMediatorName,
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
        mediatorName: AdMediatorName,
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
        mediatorName: AdMediatorName,
        placement: String?,
        adUnitId: String,
        impressionId: String,
        revenueMicros: Long,
        currency: String,
        precision: AdRevenuePrecision,
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
