@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.admob.nextgen

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
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdLoadCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdLoaderCallback
import com.revenuecat.purchases.admob.nextgen.tracking.trackAdFailedToLoad
import com.revenuecat.purchases.admob.nextgen.tracking.trackAdLoaded
import com.revenuecat.purchases.admob.nextgen.tracking.trackAndConfigureAdLoadResult
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.jvm.JvmSynthetic

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
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
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
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    adEventCallback: InterstitialAdEventCallback? = null,
): AdLoadResult<InterstitialAd> {
    val adUnitId = adRequest.adUnitId
    return InterstitialAd.load(adRequest).trackAndConfigureAdLoadResult(
        adFormat = AdFormat.INTERSTITIAL,
        placement = placement,
        adUnitId = adUnitId,
        configureAd = { ad ->
            ad.installTrackingEventCallback(
                delegate = adEventCallback,
                placement = placement,
                adUnitId = adUnitId,
            )
        },
    )
}

/**
 * Loads an [InterstitialAd] from a server-to-server ad response and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback]. [adUnitId] is required
 * because neither the opaque response nor a failed load reliably provides it. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adResponse The opaque server-to-server ad response supplied by Google Mobile Ads.
 * @param adUnitId The ad unit ID associated with [adResponse], used for RevenueCat tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for interstitial lifecycle and paid events.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackInterstitialAdFromResponse(
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    loadCallback: AdLoadCallback<InterstitialAd>,
    adEventCallback: InterstitialAdEventCallback? = null,
) {
    InterstitialAd.loadFromAdResponse(
        adResponse,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.INTERSTITIAL,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
            },
        ),
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
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for rewarded lifecycle and paid events.
 */
@JvmSynthetic
public fun AdTracker.loadAndTrackRewardedAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<RewardedAd>,
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
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
            },
        ),
    )
}

/**
 * Loads a [RewardedAd] using Google Mobile Ads' suspending API and automatically tracks RevenueCat ad events.
 *
 * The original [AdLoadResult] is returned unchanged. A successfully loaded ad has event tracking installed before
 * this function returns. Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param adEventCallback Optional callback for rewarded lifecycle and paid events.
 */
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackRewardedAd(
    adRequest: AdRequest,
    placement: String? = null,
    adEventCallback: RewardedAdEventCallback? = null,
): AdLoadResult<RewardedAd> {
    val adUnitId = adRequest.adUnitId
    return RewardedAd.load(adRequest).trackAndConfigureAdLoadResult(
        adFormat = AdFormat.REWARDED,
        placement = placement,
        adUnitId = adUnitId,
        configureAd = { ad ->
            ad.installTrackingEventCallback(
                delegate = adEventCallback,
                placement = placement,
                adUnitId = adUnitId,
            )
        },
    )
}

/**
 * Loads a [RewardedAd] from a server-to-server ad response and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback]. [adUnitId] is required
 * because neither the opaque response nor a failed load reliably provides it. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adResponse The opaque server-to-server ad response supplied by Google Mobile Ads.
 * @param adUnitId The ad unit ID associated with [adResponse], used for RevenueCat tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for rewarded lifecycle and paid events.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackRewardedAdFromResponse(
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    loadCallback: AdLoadCallback<RewardedAd>,
    adEventCallback: RewardedAdEventCallback? = null,
) {
    RewardedAd.loadFromAdResponse(
        adResponse,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.REWARDED,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
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
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for app open lifecycle and paid events.
 */
@JvmSynthetic
public fun AdTracker.loadAndTrackAppOpenAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<AppOpenAd>,
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
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
            },
        ),
    )
}

/**
 * Loads an [AppOpenAd] using Google Mobile Ads' suspending API and automatically tracks RevenueCat ad events.
 *
 * The original [AdLoadResult] is returned unchanged. A successfully loaded ad has event tracking installed before
 * this function returns. Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param adEventCallback Optional callback for app open lifecycle and paid events.
 */
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackAppOpenAd(
    adRequest: AdRequest,
    placement: String? = null,
    adEventCallback: AppOpenAdEventCallback? = null,
): AdLoadResult<AppOpenAd> {
    val adUnitId = adRequest.adUnitId
    return AppOpenAd.load(adRequest).trackAndConfigureAdLoadResult(
        adFormat = AdFormat.APP_OPEN,
        placement = placement,
        adUnitId = adUnitId,
        configureAd = { ad ->
            ad.installTrackingEventCallback(
                delegate = adEventCallback,
                placement = placement,
                adUnitId = adUnitId,
            )
        },
    )
}

/**
 * Loads an [AppOpenAd] from a server-to-server ad response and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback]. [adUnitId] is required
 * because neither the opaque response nor a failed load reliably provides it. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adResponse The opaque server-to-server ad response supplied by Google Mobile Ads.
 * @param adUnitId The ad unit ID associated with [adResponse], used for RevenueCat tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for app open lifecycle and paid events.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackAppOpenAdFromResponse(
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    loadCallback: AdLoadCallback<AppOpenAd>,
    adEventCallback: AppOpenAdEventCallback? = null,
) {
    AppOpenAd.loadFromAdResponse(
        adResponse,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.APP_OPEN,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
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
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for rewarded interstitial lifecycle, metadata, and paid events.
 */
@JvmSynthetic
public fun AdTracker.loadAndTrackRewardedInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<RewardedInterstitialAd>,
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
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
            },
        ),
    )
}

/**
 * Loads a [RewardedInterstitialAd] using Google's suspending API and automatically tracks RevenueCat ad events.
 *
 * The original [AdLoadResult] is returned unchanged. A successfully loaded ad has event tracking installed before
 * this function returns. Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param adEventCallback Optional callback for rewarded interstitial lifecycle, metadata, and paid events.
 */
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackRewardedInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    adEventCallback: RewardedInterstitialAdEventCallback? = null,
): AdLoadResult<RewardedInterstitialAd> {
    val adUnitId = adRequest.adUnitId
    return RewardedInterstitialAd.load(adRequest).trackAndConfigureAdLoadResult(
        adFormat = AdFormat.REWARDED_INTERSTITIAL,
        placement = placement,
        adUnitId = adUnitId,
        configureAd = { ad ->
            ad.installTrackingEventCallback(
                delegate = adEventCallback,
                placement = placement,
                adUnitId = adUnitId,
            )
        },
    )
}

/**
 * Loads a [RewardedInterstitialAd] from a server-to-server ad response and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback]. [adUnitId] is required
 * because neither the opaque response nor a failed load reliably provides it. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adResponse The opaque server-to-server ad response supplied by Google Mobile Ads.
 * @param adUnitId The ad unit ID associated with [adResponse], used for RevenueCat tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for rewarded interstitial lifecycle, metadata, and paid events.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackRewardedInterstitialAdFromResponse(
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    loadCallback: AdLoadCallback<RewardedInterstitialAd>,
    adEventCallback: RewardedInterstitialAdEventCallback? = null,
) {
    RewardedInterstitialAd.loadFromAdResponse(
        adResponse,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.REWARDED_INTERSTITIAL,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
            },
        ),
    )
}

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
 * Loads a banner into [adView] from a server-to-server response and tracks RevenueCat ad events.
 *
 * The loaded ad has event and refresh tracking installed before it is forwarded to [loadCallback]. [adUnitId] is
 * required because neither the opaque response nor a failed load reliably provides it. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adView The [AdView] that will display the loaded banner.
 * @param adResponse The opaque server-to-server ad response supplied by Google Mobile Ads.
 * @param adUnitId The ad unit ID associated with [adResponse], used for RevenueCat tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Required callback to receive load success and failure events and distinguish this callback-based
 * overload from its suspending counterpart.
 * @param adEventCallback Optional callback for banner lifecycle and paid events.
 * @param bannerAdRefreshCallback Optional callback for automatic banner refresh events.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackBannerAdFromResponse(
    adView: AdView,
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    loadCallback: AdLoadCallback<BannerAd>,
    adEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
) {
    adView.loadAndTrackBannerAdFromResponseInternal(
        adResponse = adResponse,
        adUnitId = adUnitId,
        placement = placement,
        loadCallback = loadCallback,
        adEventCallback = adEventCallback,
        bannerAdRefreshCallback = bannerAdRefreshCallback,
    )
}

/**
 * Loads a banner into [adView] from a server-to-server response using Google Mobile Ads' suspending API and tracks
 * RevenueCat ad events.
 *
 * The original [AdLoadResult] is returned unchanged. A successfully loaded ad has event and refresh tracking
 * installed before this function returns. [adUnitId] is required because neither the opaque response nor a failed
 * load reliably provides it. Call via `Purchases.sharedInstance.adTracker`.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public suspend fun AdTracker.loadAndTrackBannerAdFromResponse(
    adView: AdView,
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    adEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
): AdLoadResult<BannerAd> = adView.loadAndTrackBannerAdFromResponseInternal(
    adResponse = adResponse,
    adUnitId = adUnitId,
    placement = placement,
    adEventCallback = adEventCallback,
    bannerAdRefreshCallback = bannerAdRefreshCallback,
)

/**
 * Loads one native-ad request and automatically tracks every result it can produce.
 *
 * A [NativeAdRequest] can return a standard native, custom-native, or banner ad. The matching event tracking is
 * installed before that result is forwarded to [loadCallback]. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The native ad request. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive the load result. This parameter is required to distinguish this
 * callback-based overload from the suspending overload.
 * @param nativeAdEventCallback Optional lifecycle and paid-event callback for standard and custom-native results.
 * @param bannerAdEventCallback Optional lifecycle and paid-event callback for banner results.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackNativeAd(
    adRequest: NativeAdRequest,
    placement: String? = null,
    loadCallback: NativeAdLoaderCallback,
    nativeAdEventCallback: NativeAdEventCallback? = null,
    bannerAdEventCallback: BannerAdEventCallback? = null,
) {
    NativeAdLoader.load(
        adRequest,
        trackingNativeAdLoaderCallback(
            delegate = loadCallback,
            placement = placement,
            adUnitId = adRequest.adUnitId,
            nativeAdEventCallback = nativeAdEventCallback,
            bannerAdEventCallback = bannerAdEventCallback,
        ),
    )
}

/**
 * Loads one native-ad request with Google's suspending API and automatically tracks its result.
 *
 * The original [NativeAdLoadResult] is returned unchanged. Event tracking is installed on every successful result
 * before this function returns. Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The native ad request. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param nativeAdEventCallback Optional lifecycle and paid-event callback for standard and custom-native results.
 * @param bannerAdEventCallback Optional lifecycle and paid-event callback for banner results.
 */
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackNativeAd(
    adRequest: NativeAdRequest,
    placement: String? = null,
    nativeAdEventCallback: NativeAdEventCallback? = null,
    bannerAdEventCallback: BannerAdEventCallback? = null,
): NativeAdLoadResult {
    val result = NativeAdLoader.load(adRequest)
    trackAndConfigureNativeAdLoadResult(
        result = result,
        placement = placement,
        adUnitId = adRequest.adUnitId,
        nativeAdEventCallback = nativeAdEventCallback,
        bannerAdEventCallback = bannerAdEventCallback,
    )
    return result
}

/**
 * Loads one native ad from a server-to-server response and automatically tracks every possible result.
 *
 * Event tracking is installed before a successful result is forwarded to [loadCallback]. [adUnitId] is
 * required because neither the opaque response nor a failed load reliably provides it. Call via
 * `Purchases.sharedInstance.adTracker`.
 *
 * @param adResponse The opaque server-to-server ad response supplied by Google Mobile Ads.
 * @param adUnitId The ad unit ID associated with [adResponse], used for RevenueCat tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive the load result.
 * @param nativeAdEventCallback Optional lifecycle and paid-event callback for standard and custom-native results.
 * @param bannerAdEventCallback Optional lifecycle and paid-event callback for banner results.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackNativeAdFromResponse(
    adResponse: String,
    adUnitId: String,
    placement: String? = null,
    loadCallback: NativeAdLoaderCallback,
    nativeAdEventCallback: NativeAdEventCallback? = null,
    bannerAdEventCallback: BannerAdEventCallback? = null,
) {
    NativeAdLoader.loadFromAdResponse(
        adResponse,
        trackingNativeAdLoaderCallback(
            delegate = loadCallback,
            placement = placement,
            adUnitId = adUnitId,
            nativeAdEventCallback = nativeAdEventCallback,
            bannerAdEventCallback = bannerAdEventCallback,
        ),
    )
}

/**
 * Loads up to [maxNumberOfAds] native ads and automatically tracks every callback result.
 *
 * Google can return standard native, custom-native, banner, and failure results in one batch. Each callback is
 * forwarded after RevenueCat tracks and configures its result; [NativeAdLoaderCallback.onAdLoadingCompleted] is
 * forwarded when Google finishes the batch. [loadCallback] is required to distinguish this callback-based
 * overload from the suspending overload.
 */
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdTracker.loadAndTrackNativeAds(
    adRequest: NativeAdRequest,
    maxNumberOfAds: Int,
    placement: String? = null,
    loadCallback: NativeAdLoaderCallback,
    nativeAdEventCallback: NativeAdEventCallback? = null,
    bannerAdEventCallback: BannerAdEventCallback? = null,
) {
    NativeAdLoader.load(
        adRequest,
        maxNumberOfAds,
        trackingNativeAdLoaderCallback(
            delegate = loadCallback,
            placement = placement,
            adUnitId = adRequest.adUnitId,
            nativeAdEventCallback = nativeAdEventCallback,
            bannerAdEventCallback = bannerAdEventCallback,
        ),
    )
}

/**
 * Loads up to [maxNumberOfAds] native ads and tracks every result emitted by Google's original [Flow].
 *
 * Results are returned unchanged and tracked when collected. Event tracking is installed on each successful result
 * before it is emitted downstream.
 */
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackNativeAds(
    adRequest: NativeAdRequest,
    maxNumberOfAds: Int,
    placement: String? = null,
    nativeAdEventCallback: NativeAdEventCallback? = null,
    bannerAdEventCallback: BannerAdEventCallback? = null,
): Flow<NativeAdLoadResult> {
    val adUnitId = adRequest.adUnitId
    return NativeAdLoader.load(adRequest, maxNumberOfAds).onEach { result ->
        trackAndConfigureNativeAdLoadResult(
            result = result,
            placement = placement,
            adUnitId = adUnitId,
            nativeAdEventCallback = nativeAdEventCallback,
            bannerAdEventCallback = bannerAdEventCallback,
        )
    }
}

private fun trackingNativeAdLoaderCallback(
    delegate: NativeAdLoaderCallback,
    placement: String?,
    adUnitId: String,
    nativeAdEventCallback: NativeAdEventCallback?,
    bannerAdEventCallback: BannerAdEventCallback?,
): TrackingNativeAdLoaderCallback = TrackingNativeAdLoaderCallback(
    delegate = delegate,
    placement = placement,
    adUnitId = adUnitId,
    configureAd = { ad -> ad.installTrackingEventCallback(nativeAdEventCallback, placement, adUnitId) },
    configureCustomNativeAd = { ad ->
        ad.installTrackingEventCallback(nativeAdEventCallback, placement, adUnitId)
    },
    configureBannerAd = { ad -> ad.installTrackingCallbacks(bannerAdEventCallback, null, placement, adUnitId) },
)

private fun trackAndConfigureNativeAdLoadResult(
    result: NativeAdLoadResult,
    placement: String?,
    adUnitId: String,
    nativeAdEventCallback: NativeAdEventCallback?,
    bannerAdEventCallback: BannerAdEventCallback?,
) {
    when (result) {
        is NativeAdLoadResult.NativeAdSuccess -> {
            trackAdLoaded({ result.ad.getResponseInfo() }, AdFormat.NATIVE, placement, adUnitId)
            result.ad.installTrackingEventCallback(nativeAdEventCallback, placement, adUnitId)
        }
        is NativeAdLoadResult.CustomNativeAdSuccess -> {
            trackAdLoaded({ result.ad.getResponseInfo() }, AdFormat.NATIVE, placement, adUnitId)
            result.ad.installTrackingEventCallback(nativeAdEventCallback, placement, adUnitId)
        }
        is NativeAdLoadResult.BannerAdSuccess -> {
            trackAdLoaded({ result.ad.getResponseInfo() }, AdFormat.BANNER, placement, adUnitId)
            result.ad.installTrackingCallbacks(bannerAdEventCallback, null, placement, adUnitId)
        }
        is NativeAdLoadResult.Failure -> {
            trackAdFailedToLoad(result.error, AdFormat.NATIVE, placement, adUnitId)
        }
    }
}
