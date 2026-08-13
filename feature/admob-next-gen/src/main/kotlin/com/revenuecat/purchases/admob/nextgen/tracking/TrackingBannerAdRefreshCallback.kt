package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingBannerAdRefreshCallback(
    internal var delegate: BannerAdRefreshCallback?,
    private val placement: String?,
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
