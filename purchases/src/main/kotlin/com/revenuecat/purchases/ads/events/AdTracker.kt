package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.EventsManager

@InternalRevenueCatAPI
class AdTracker internal constructor(
    private val eventsManager: EventsManager?,
) {

    fun trackAdDisplayed(
        networkName: String,
        mediatorName: String,
        placement: String?,
        adUnitId: String,
        adInstanceId: String,
    ) {
        eventsManager?.track(
            event = AdEvent.Displayed(
                networkName = networkName,
                mediatorName = mediatorName,
                placement = placement,
                adUnitId = adUnitId,
                adInstanceId = adInstanceId,
            ),
        )
    }

    fun trackAdOpened(
        networkName: String,
        mediatorName: String,
        placement: String?,
        adUnitId: String,
        adInstanceId: String,
    ) {
        eventsManager?.track(
            event = AdEvent.Open(
                networkName = networkName,
                mediatorName = mediatorName,
                placement = placement,
                adUnitId = adUnitId,
                adInstanceId = adInstanceId,
            ),
        )
    }

    @Suppress("LongParameterList")
    fun trackAdRevenue(
        networkName: String,
        mediatorName: String,
        placement: String?,
        adUnitId: String,
        adInstanceId: String,
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
                adInstanceId = adInstanceId,
                revenueMicros = revenueMicros,
                currency = currency,
                precision = precision,
            ),
        )
    }
}
