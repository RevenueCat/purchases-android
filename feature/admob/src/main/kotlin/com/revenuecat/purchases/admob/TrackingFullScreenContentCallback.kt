@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData

/**
 * A [FullScreenContentCallback] wrapper that injects RevenueCat ad-event tracking
 * before delegating every callback to the user-provided [delegate].
 *
 * Tracked events:
 * - [onAdShowedFullScreenContent] → `trackAdDisplayed`
 * - [onAdClicked] → `trackAdOpened`
 *
 * Revenue is tracked separately via [OnPaidEventListener] which must be wired
 * on the ad object by the caller (see [setUpPaidEventTracking]).
 */
internal class TrackingFullScreenContentCallback(
    private val delegate: FullScreenContentCallback?,
    private val adFormat: AdFormat,
    private val placement: String?,
    private val adUnitId: String,
    private val responseInfoProvider: () -> ResponseInfo,
) : FullScreenContentCallback() {

    override fun onAdShowedFullScreenContent() {
        val responseInfo = responseInfoProvider()
        trackIfConfigured {
            adTracker.trackAdDisplayed(
                AdDisplayedData(
                    networkName = responseInfo.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = responseInfo.responseId.orEmpty(),
                ),
            )
        }
        delegate?.onAdShowedFullScreenContent()
    }

    override fun onAdClicked() {
        val responseInfo = responseInfoProvider()
        trackIfConfigured {
            adTracker.trackAdOpened(
                AdOpenedData(
                    networkName = responseInfo.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = responseInfo.responseId.orEmpty(),
                ),
            )
        }
        delegate?.onAdClicked()
    }

    override fun onAdDismissedFullScreenContent() {
        delegate?.onAdDismissedFullScreenContent()
    }

    override fun onAdFailedToShowFullScreenContent(error: AdError) {
        delegate?.onAdFailedToShowFullScreenContent(error)
    }

    override fun onAdImpression() {
        delegate?.onAdImpression()
    }
}

/**
 * Wires [OnPaidEventListener] for revenue tracking on a full-screen ad.
 */
@Suppress("LongParameterList")
internal fun setUpPaidEventTracking(
    setListener: (OnPaidEventListener) -> Unit,
    adFormat: AdFormat,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
    delegate: OnPaidEventListener? = null,
) {
    setListener(
        OnPaidEventListener { adValue ->
            val responseInfo = responseInfoProvider()
            trackIfConfigured {
                adTracker.trackAdRevenue(
                    AdRevenueData(
                        networkName = responseInfo.mediationAdapterClassName,
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = adFormat,
                        placement = placement,
                        adUnitId = adUnitId,
                        impressionId = responseInfo.responseId.orEmpty(),
                        revenueMicros = adValue.valueMicros,
                        currency = adValue.currencyCode,
                        precision = adValue.precisionType.toAdRevenuePrecision(),
                    ),
                )
            }
            delegate?.onPaidEvent(adValue)
        },
    )
}
