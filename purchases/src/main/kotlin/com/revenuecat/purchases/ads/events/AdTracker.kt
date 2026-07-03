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
 *
 * Events tracked through the public `trackAd*` API are stamped with
 * [AdCaptureMethod.MANUAL]. RevenueCat ad-network adapters use the
 * [InternalRevenueCatAPI] overloads to stamp [AdCaptureMethod.ADAPTER] instead.
 */
@OptIn(InternalRevenueCatAPI::class)
@ExperimentalPreviewRevenueCatPurchasesAPI
public class AdTracker internal constructor(
    private val eventsManager: EventsManager,
) {

    /**
     * Tracks an ad displayed event.
     *
     * @param data The ad display event data.
     */
    public fun trackAdDisplayed(data: AdDisplayedData): Unit =
        trackAdDisplayed(data, AdCaptureMethod.MANUAL)

    /**
     * Tracks an ad displayed event, stamping the capture method that emitted it.
     */
    @InternalRevenueCatAPI
    public fun trackAdDisplayed(data: AdDisplayedData, captureMethod: AdCaptureMethod) {
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
     */
    public fun trackAdOpened(data: AdOpenedData): Unit =
        trackAdOpened(data, AdCaptureMethod.MANUAL)

    /**
     * Tracks an ad opened event, stamping the capture method that emitted it.
     */
    @InternalRevenueCatAPI
    public fun trackAdOpened(data: AdOpenedData, captureMethod: AdCaptureMethod) {
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
     */
    public fun trackAdRevenue(data: AdRevenueData): Unit =
        trackAdRevenue(data, AdCaptureMethod.MANUAL)

    /**
     * Tracks an ad revenue event, stamping the capture method that emitted it.
     */
    @InternalRevenueCatAPI
    public fun trackAdRevenue(data: AdRevenueData, captureMethod: AdCaptureMethod) {
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
     */
    public fun trackAdLoaded(data: AdLoadedData): Unit =
        trackAdLoaded(data, AdCaptureMethod.MANUAL)

    /**
     * Tracks an ad loaded event, stamping the capture method that emitted it.
     */
    @InternalRevenueCatAPI
    public fun trackAdLoaded(data: AdLoadedData, captureMethod: AdCaptureMethod) {
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
     */
    public fun trackAdFailedToLoad(data: AdFailedToLoadData): Unit =
        trackAdFailedToLoad(data, AdCaptureMethod.MANUAL)

    /**
     * Tracks an ad failed to load event, stamping the capture method that emitted it.
     */
    @InternalRevenueCatAPI
    public fun trackAdFailedToLoad(data: AdFailedToLoadData, captureMethod: AdCaptureMethod) {
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
