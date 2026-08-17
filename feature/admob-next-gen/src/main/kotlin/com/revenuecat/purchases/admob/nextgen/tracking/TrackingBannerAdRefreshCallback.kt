package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
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
 * [placement] is a var for the same reason. A banner refreshes for as long as it is
 * on screen, so a placement captured at construction would keep reporting the
 * load-time value after [TrackingAdEventCallback.placement] had already moved on,
 * splitting one banner's events across two placements.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingBannerAdRefreshCallback(
    internal var delegate: BannerAdRefreshCallback?,
    internal var placement: String?,
    private val adUnitId: String,
    private val responseInfoProvider: () -> ResponseInfo,
) : BannerAdRefreshCallback {

    override fun onAdRefreshed() {
        trackAdLoaded(responseInfoProvider(), AdFormat.BANNER, placement, adUnitId)
        delegate?.onAdRefreshed()
    }

    override fun onAdFailedToRefresh(adError: LoadAdError) {
        trackAdFailedToLoad(adError, AdFormat.BANNER, placement, adUnitId)
        delegate?.onAdFailedToRefresh(adError)
    }
}
