package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.common.events.EventsManager

/**
 * Tracks ad-related events such as ad displays, opens, and revenue.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
@ExperimentalPreviewRevenueCatPurchasesAPI
public class AdTracker internal constructor(
    private val eventsManager: EventsManager,
) {

    /**
     * Tracks an ad displayed event.
     *
     * @param data The ad display event data.
     * @param captureMethod The mechanism that emitted the event. Defaults to [AdCaptureMethod.MANUAL].
     */
    public fun trackAdDisplayed(
        data: AdDisplayedData,
        captureMethod: AdCaptureMethod = AdCaptureMethod.MANUAL,
    ) {
        eventsManager.track(
            event = AdEvent.Displayed(
                networkName = data.networkName,
                mediatorName = data.mediatorName,
                adFormat = data.adFormat,
                placement = data.placement,
                adUnitId = data.adUnitId,
                impressionId = data.impressionId,
                captureMethod = captureMethod,
            ),
        )
    }

    /**
     * Tracks an ad opened event.
     *
     * @param data The ad open event data.
     * @param captureMethod The mechanism that emitted the event. Defaults to [AdCaptureMethod.MANUAL].
     */
    public fun trackAdOpened(
        data: AdOpenedData,
        captureMethod: AdCaptureMethod = AdCaptureMethod.MANUAL,
    ) {
        eventsManager.track(
            event = AdEvent.Open(
                networkName = data.networkName,
                mediatorName = data.mediatorName,
                adFormat = data.adFormat,
                placement = data.placement,
                adUnitId = data.adUnitId,
                impressionId = data.impressionId,
                captureMethod = captureMethod,
            ),
        )
    }

    /**
     * Tracks an ad revenue event.
     *
     * @param data The ad revenue event data.
     * @param captureMethod The mechanism that emitted the event. Defaults to [AdCaptureMethod.MANUAL].
     */
    public fun trackAdRevenue(
        data: AdRevenueData,
        captureMethod: AdCaptureMethod = AdCaptureMethod.MANUAL,
    ) {
        eventsManager.track(
            event = AdEvent.Revenue(
                networkName = data.networkName,
                mediatorName = data.mediatorName,
                adFormat = data.adFormat,
                placement = data.placement,
                adUnitId = data.adUnitId,
                impressionId = data.impressionId,
                captureMethod = captureMethod,
                revenueMicros = data.revenueMicros,
                currency = data.currency,
                precision = data.precision,
            ),
        )
    }

    /**
     * Tracks an ad loaded event.
     *
     * @param data The ad loaded event data.
     * @param captureMethod The mechanism that emitted the event. Defaults to [AdCaptureMethod.MANUAL].
     */
    public fun trackAdLoaded(
        data: AdLoadedData,
        captureMethod: AdCaptureMethod = AdCaptureMethod.MANUAL,
    ) {
        eventsManager.track(
            event = AdEvent.Loaded(
                networkName = data.networkName,
                mediatorName = data.mediatorName,
                adFormat = data.adFormat,
                placement = data.placement,
                adUnitId = data.adUnitId,
                impressionId = data.impressionId,
                captureMethod = captureMethod,
            ),
        )
    }

    /**
     * Tracks an ad failed to load event.
     *
     * @param data The ad failed to load event data.
     * @param captureMethod The mechanism that emitted the event. Defaults to [AdCaptureMethod.MANUAL].
     */
    public fun trackAdFailedToLoad(
        data: AdFailedToLoadData,
        captureMethod: AdCaptureMethod = AdCaptureMethod.MANUAL,
    ) {
        eventsManager.track(
            event = AdEvent.FailedToLoad(
                mediatorName = data.mediatorName,
                adFormat = data.adFormat,
                placement = data.placement,
                adUnitId = data.adUnitId,
                impressionId = null,
                captureMethod = captureMethod,
                mediatorErrorCode = data.mediatorErrorCode,
            ),
        )
    }
}
