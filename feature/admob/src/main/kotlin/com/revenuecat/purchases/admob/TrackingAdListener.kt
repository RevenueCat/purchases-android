@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData

/**
 * An [AdListener] wrapper that injects RevenueCat ad-event tracking before
 * delegating every callback to the user-provided [delegate].
 *
 * Tracked events:
 * - [onAdLoaded] → `trackAdLoaded`
 * - [onAdImpression] → `trackAdDisplayed`
 * - [onAdClicked] → `trackAdOpened`
 * - [onAdFailedToLoad] → `trackAdFailedToLoad`
 *
 * Revenue is tracked separately via [com.google.android.gms.ads.OnPaidEventListener],
 * which must be wired on the ad view by the caller.
 */
internal class TrackingAdListener(
    internal val delegate: AdListener?,
    private val adFormat: AdFormat,
    private val placement: String?,
    private val adUnitId: String,
    private val responseInfoProvider: () -> ResponseInfo?,
) : AdListener() {

    override fun onAdLoaded() {
        val responseInfo = responseInfoProvider()
        trackIfConfigured {
            adTracker.trackAdLoaded(
                AdLoadedData(
                    networkName = responseInfo?.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = responseInfo?.responseId.orEmpty(),
                ),
            )
        }
        delegate?.onAdLoaded()
    }

    override fun onAdImpression() {
        val responseInfo = responseInfoProvider()
        trackIfConfigured {
            adTracker.trackAdDisplayed(
                AdDisplayedData(
                    networkName = responseInfo?.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = responseInfo?.responseId.orEmpty(),
                ),
            )
        }
        delegate?.onAdImpression()
    }

    override fun onAdClicked() {
        val responseInfo = responseInfoProvider()
        trackIfConfigured {
            adTracker.trackAdOpened(
                AdOpenedData(
                    networkName = responseInfo?.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = responseInfo?.responseId.orEmpty(),
                ),
            )
        }
        delegate?.onAdClicked()
    }

    override fun onAdFailedToLoad(error: LoadAdError) {
        trackIfConfigured {
            adTracker.trackAdFailedToLoad(
                AdFailedToLoadData(
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    mediatorErrorCode = error.code,
                ),
            )
        }
        delegate?.onAdFailedToLoad(error)
    }

    override fun onAdClosed() {
        delegate?.onAdClosed()
    }

    override fun onAdOpened() {
        delegate?.onAdOpened()
    }

    override fun onAdSwipeGestureClicked() {
        delegate?.onAdSwipeGestureClicked()
    }
}
