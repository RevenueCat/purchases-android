@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdLoadCallback
import com.revenuecat.purchases.admob.nextgen.tracking.trackAndConfigureAdLoadResult
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
@ExperimentalPreviewRevenueCatPurchasesAPI
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
