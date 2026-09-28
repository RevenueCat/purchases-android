package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ads.events.types.AdFormat

/**
 * A [PreloadCallback] wrapper that tracks every completed preload attempt before
 * forwarding the callback to the user-provided [delegate].
 */
internal class TrackingPreloadCallback(
    private val delegate: PreloadCallback?,
    private val adFormat: AdFormat,
    private val placement: String?,
    private val adUnitId: String,
) : PreloadCallback {

    override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
        trackAdLoaded({ responseInfo }, adFormat, placement, adUnitId)
        delegate?.onAdPreloaded(preloadId, responseInfo)
    }

    override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
        trackAdFailedToLoad(adError, adFormat, placement, adUnitId)
        delegate?.onAdFailedToPreload(preloadId, adError)
    }

    override fun onAdsExhausted(preloadId: String) {
        delegate?.onAdsExhausted(preloadId)
    }
}
