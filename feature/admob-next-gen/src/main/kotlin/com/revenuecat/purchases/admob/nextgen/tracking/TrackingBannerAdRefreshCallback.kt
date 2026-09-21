package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ads.events.types.AdFormat

/**
 * A [BannerAdRefreshCallback] wrapper that injects RevenueCat ad-event tracking
 * before delegating every callback to the user-provided [delegate].
 *
 * Banner refreshes are reported as load lifecycle events:
 * - [onAdRefreshed] → `trackAdLoaded`
 * - [onAdFailedToRefresh] → `trackAdFailedToLoad`
 *
 * The SDK does not pass the new [ResponseInfo] to [onAdRefreshed], so
 * [responseInfoProvider] is read when the callback fires rather than captured up
 * front, to report the refreshed response instead of the original one.
 *
 * [placement] stays immutable, unlike [TrackingAdEventCallback.placement]. A show-time
 * placement override only exists for the full-screen formats, which have no refresh
 * callback, so a banner reports the one placement it was loaded with.
 *
 * [delegate] is `@Volatile` because the app replaces it from whatever thread it calls
 * `setTrackingBannerAdRefreshCallback` on, while the SDK reads it on the background thread
 * it invokes refresh callbacks from.
 */
internal class TrackingBannerAdRefreshCallback(
    @Volatile internal var delegate: BannerAdRefreshCallback?,
    private val placement: String?,
    private val adUnitId: String,
    private val responseInfoProvider: () -> ResponseInfo,
) : BannerAdRefreshCallback {

    override fun onAdRefreshed() {
        trackAdLoaded(responseInfoProvider, AdFormat.BANNER, placement, adUnitId)
        delegate?.onAdRefreshed()
    }

    override fun onAdFailedToRefresh(adError: LoadAdError) {
        trackAdFailedToLoad(adError, AdFormat.BANNER, placement, adUnitId)
        delegate?.onAdFailedToRefresh(adError)
    }
}
