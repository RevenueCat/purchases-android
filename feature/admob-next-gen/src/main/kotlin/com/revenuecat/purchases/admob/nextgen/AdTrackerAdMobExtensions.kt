@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob.nextgen

import android.annotation.SuppressLint
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdLoadCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Sets up RevenueCat ad-event tracking for the given [AdView] and loads a banner ad.
 *
 * Call via `Purchases.sharedInstance.adTracker`. For a call that does not take the tracker
 * explicitly, use [AdView.loadAndTrackAd].
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

/**
 * Loads an [InterstitialAd] and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback].
 * Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success and failure events.
 * @param adEventCallback Optional callback for interstitial lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<InterstitialAd>? = null,
    adEventCallback: InterstitialAdEventCallback? = null,
) {
    val adUnitId = adRequest.adUnitId
    InterstitialAd.load(
        adRequest,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.INTERSTITIAL,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.adEventCallback = TrackingInterstitialAdEventCallback(
                    initialDelegate = adEventCallback,
                    initialPlacement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = ad::getResponseInfo,
                )
            },
        ),
    )
}
