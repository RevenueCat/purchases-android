package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName

/**
 * An [AdLoadCallback] wrapper that injects RevenueCat ad-event tracking before
 * delegating every callback to the user-provided [delegate].
 *
 * Tracked events:
 * - [onAdLoaded] → `trackAdLoaded`
 * - [onAdFailedToLoad] → `trackAdFailedToLoad`
 *
 * [configureAd] runs on a successfully loaded ad, after tracking and before the
 * delegate, so the caller can wire up the tracking that the ad object itself
 * exposes. It is not invoked for an ad that failed to load.
 */
internal class TrackingAdLoadCallback<AdT : Ad>(
    private val delegate: AdLoadCallback<AdT>?,
    private val adFormat: AdFormat,
    private val placement: String?,
    private val adUnitId: String,
    private val configureAd: (AdT) -> Unit,
) : AdLoadCallback<AdT> {

    override fun onAdLoaded(ad: AdT) {
        trackAdLoaded({ ad.getResponseInfo() }, adFormat, placement, adUnitId)
        configureAd(ad)
        delegate?.onAdLoaded(ad)
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        trackAdFailedToLoad(adError, adFormat, placement, adUnitId)
        delegate?.onAdFailedToLoad(adError)
    }
}

internal fun <AdT : Ad> AdLoadResult<AdT>.trackAndConfigureAdLoadResult(
    adFormat: AdFormat,
    placement: String?,
    adUnitId: String,
    configureAd: (AdT) -> Unit,
): AdLoadResult<AdT> {
    when (this) {
        is AdLoadResult.Success -> {
            trackAdLoaded({ ad.getResponseInfo() }, adFormat, placement, adUnitId)
            configureAd(ad)
        }
        is AdLoadResult.Failure -> trackAdFailedToLoad(error, adFormat, placement, adUnitId)
    }
    return this
}

internal fun trackAdLoaded(
    responseInfoProvider: () -> ResponseInfo,
    adFormat: AdFormat,
    placement: String?,
    adUnitId: String,
) {
    trackIfConfigured {
        val responseInfo = responseInfoProvider()
        adTracker.trackFromAdapter(
            AdLoadedData(
                networkName = responseInfo.adapterClassName,
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = adFormat,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = responseInfo.responseId.orEmpty(),
            ),
        )
    }
}

internal fun trackAdFailedToLoad(
    adError: LoadAdError,
    adFormat: AdFormat,
    placement: String?,
    adUnitId: String,
) {
    trackIfConfigured {
        adTracker.trackFromAdapter(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = adFormat,
                placement = placement,
                adUnitId = adUnitId,
                mediatorErrorCode = adError.code.value,
            ),
        )
    }
}
