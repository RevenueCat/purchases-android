@file:JvmName("AdTrackerAdMobNextGen")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob.nextgen

import android.annotation.SuppressLint
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdLoadCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdLoaderCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.trackAdFailedToLoad
import com.revenuecat.purchases.admob.nextgen.tracking.trackAdLoaded
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
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for interstitial lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<InterstitialAd>,
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
                ad.installTrackingEventCallback(adEventCallback, placement, adUnitId)
            },
        ),
    )
}

/**
 * Loads an [InterstitialAd] using Google Mobile Ads' suspending API and automatically tracks RevenueCat ad events.
 *
 * The original [AdLoadResult] is returned unchanged. A successfully loaded ad has event tracking installed before
 * this function returns. Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param adEventCallback Optional callback for interstitial lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    adEventCallback: InterstitialAdEventCallback? = null,
): AdLoadResult<InterstitialAd> {
    val adUnitId = adRequest.adUnitId
    val result = InterstitialAd.load(adRequest)

    when (result) {
        is AdLoadResult.Success -> {
            trackAdLoaded(result.ad.getResponseInfo(), AdFormat.INTERSTITIAL, placement, adUnitId)
            result.ad.installTrackingEventCallback(adEventCallback, placement, adUnitId)
        }
        is AdLoadResult.Failure -> {
            trackAdFailedToLoad(result.error, AdFormat.INTERSTITIAL, placement, adUnitId)
        }
    }

    return result
}

private fun InterstitialAd.installTrackingEventCallback(
    delegate: InterstitialAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingInterstitialAdEventCallback(
        delegate = delegate,
        placement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}

/**
 * Loads a [RewardedAd] and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback].
 * Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success and failure events.
 * @param adEventCallback Optional callback for rewarded ad lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackRewardedAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<RewardedAd>? = null,
    adEventCallback: RewardedAdEventCallback? = null,
) {
    val adUnitId = adRequest.adUnitId
    RewardedAd.load(
        adRequest,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.REWARDED,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.adEventCallback = TrackingRewardedAdEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = ad::getResponseInfo,
                )
            },
        ),
    )
}

/**
 * Loads a [RewardedInterstitialAd] and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback].
 * Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success and failure events.
 * @param adEventCallback Optional callback for rewarded interstitial lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackRewardedInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<RewardedInterstitialAd>? = null,
    adEventCallback: RewardedInterstitialAdEventCallback? = null,
) {
    val adUnitId = adRequest.adUnitId
    RewardedInterstitialAd.load(
        adRequest,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.REWARDED_INTERSTITIAL,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.adEventCallback = TrackingRewardedInterstitialAdEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = ad::getResponseInfo,
                )
            },
        ),
    )
}

/**
 * Loads an [AppOpenAd] and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback].
 * Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success and failure events.
 * @param adEventCallback Optional callback for app open ad lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackAppOpenAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<AppOpenAd>? = null,
    adEventCallback: AppOpenAdEventCallback? = null,
) {
    val adUnitId = adRequest.adUnitId
    AppOpenAd.load(
        adRequest,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.APP_OPEN,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.adEventCallback = TrackingAppOpenAdEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = ad::getResponseInfo,
                )
            },
        ),
    )
}

/**
 * Loads a native ad and automatically tracks RevenueCat ad events.
 *
 * The loaded native ad has event tracking installed before it is forwarded to
 * [nativeAdLoaderCallback]. Custom native and banner results are forwarded unchanged.
 * Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [NativeAdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param nativeAdLoaderCallback Optional callback to receive native load results.
 * @param adEventCallback Optional callback for native ad lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackNativeAd(
    adRequest: NativeAdRequest,
    placement: String? = null,
    nativeAdLoaderCallback: NativeAdLoaderCallback? = null,
    adEventCallback: NativeAdEventCallback? = null,
) {
    NativeAdLoader.load(
        adRequest,
        TrackingNativeAdLoaderCallback(
            delegate = nativeAdLoaderCallback,
            placement = placement,
            adUnitId = adRequest.adUnitId,
            adEventCallback = adEventCallback,
        ),
    )
}
