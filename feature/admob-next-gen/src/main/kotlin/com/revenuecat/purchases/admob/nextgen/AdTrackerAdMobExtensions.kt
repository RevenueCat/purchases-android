@file:JvmName("AdTrackerAdMobNextGen")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.AdTracker
import kotlin.jvm.JvmSynthetic

/**
 * Sets up RevenueCat ad-event tracking for [adView] and loads a banner ad.
 *
 * The loaded ad has event and refresh tracking installed before it is forwarded to [loadCallback].
 * Call via `Purchases.sharedInstance.adTracker`. For a call directly on the view, use [AdView.loadAndTrackAd].
 *
 * @param adView The [AdView] that will display the loaded banner.
 * @param adRequest The [BannerAdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success and failure events.
 * @param adEventCallback Optional callback for banner lifecycle and paid events.
 * @param bannerAdRefreshCallback Optional callback for automatic banner refresh events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackBannerAd(
    adView: AdView,
    adRequest: BannerAdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<BannerAd>? = null,
    adEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
) {
    adView.loadAndTrackBannerAdInternal(
        adRequest = adRequest,
        placement = placement,
        loadCallback = loadCallback,
        adEventCallback = adEventCallback,
        bannerAdRefreshCallback = bannerAdRefreshCallback,
    )
}
