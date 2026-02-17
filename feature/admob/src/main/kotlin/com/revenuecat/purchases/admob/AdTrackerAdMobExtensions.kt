@file:JvmName("AdTrackerAdMob")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import kotlin.jvm.JvmSynthetic

/**
 * Loads an [InterstitialAd] and automatically tracks RevenueCat ad events.
 *
 * On success the ad is delivered via [loadCallback] with full-screen content
 * tracking already wired. Revenue tracking is set up via `OnPaidEventListener`.
 *
 * Call via [Purchases.sharedInstance.adTracker].
 *
 * @param context The context used to load the ad.
 * @param adUnitId The AdMob ad unit ID.
 * @param adRequest The [AdRequest] to use.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success/failure events.
 * @param fullScreenContentCallback Optional callback for full-screen ad lifecycle events.
 * @param onPaidEventListener Optional paid-event callback. RevenueCat tracking runs first.
 */
@Suppress("LongParameterList")
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackInterstitialAd(
    context: Context,
    adUnitId: String,
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: InterstitialAdLoadCallback? = null,
    fullScreenContentCallback: FullScreenContentCallback? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    InterstitialAd.load(
        context,
        adUnitId,
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                trackIfConfigured {
                    adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = ad.responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.INTERSTITIAL,
                            placement = placement,
                            adUnitId = adUnitId,
                            impressionId = ad.responseInfo.responseId.orEmpty(),
                        ),
                    )
                }
                ad.fullScreenContentCallback = TrackingFullScreenContentCallback(
                    delegate = fullScreenContentCallback,
                    adFormat = AdFormat.INTERSTITIAL,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                )
                setUpPaidEventTracking(
                    setListener = { ad.onPaidEventListener = it },
                    adFormat = AdFormat.INTERSTITIAL,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                    delegate = onPaidEventListener,
                )
                loadCallback?.onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                trackIfConfigured {
                    adTracker.trackAdFailedToLoad(
                        AdFailedToLoadData(
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.INTERSTITIAL,
                            placement = placement,
                            adUnitId = adUnitId,
                            mediatorErrorCode = error.code,
                        ),
                    )
                }
                loadCallback?.onAdFailedToLoad(error)
            }
        },
    )
}

/**
 * Loads an [AppOpenAd] and automatically tracks RevenueCat ad events.
 *
 * Call via [Purchases.sharedInstance.adTracker].
 *
 * @param context The context used to load the ad.
 * @param adUnitId The AdMob ad unit ID.
 * @param adRequest The [AdRequest] to use.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success/failure events.
 * @param fullScreenContentCallback Optional callback for full-screen ad lifecycle events.
 * @param onPaidEventListener Optional paid-event callback. RevenueCat tracking runs first.
 */
@Suppress("LongParameterList")
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackAppOpenAd(
    context: Context,
    adUnitId: String,
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null,
    fullScreenContentCallback: FullScreenContentCallback? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    AppOpenAd.load(
        context,
        adUnitId,
        adRequest,
        object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                trackIfConfigured {
                    adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = ad.responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.APP_OPEN,
                            placement = placement,
                            adUnitId = adUnitId,
                            impressionId = ad.responseInfo.responseId.orEmpty(),
                        ),
                    )
                }
                ad.fullScreenContentCallback = TrackingFullScreenContentCallback(
                    delegate = fullScreenContentCallback,
                    adFormat = AdFormat.APP_OPEN,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                )
                setUpPaidEventTracking(
                    setListener = { ad.onPaidEventListener = it },
                    adFormat = AdFormat.APP_OPEN,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                    delegate = onPaidEventListener,
                )
                loadCallback?.onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                trackIfConfigured {
                    adTracker.trackAdFailedToLoad(
                        AdFailedToLoadData(
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.APP_OPEN,
                            placement = placement,
                            adUnitId = adUnitId,
                            mediatorErrorCode = error.code,
                        ),
                    )
                }
                loadCallback?.onAdFailedToLoad(error)
            }
        },
    )
}

/**
 * Loads a [RewardedAd] and automatically tracks RevenueCat ad events.
 *
 * Call via [Purchases.sharedInstance.adTracker].
 *
 * @param context The context used to load the ad.
 * @param adUnitId The AdMob ad unit ID.
 * @param adRequest The [AdRequest] to use.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success/failure events.
 * @param fullScreenContentCallback Optional callback for full-screen ad lifecycle events.
 * @param onPaidEventListener Optional paid-event callback. RevenueCat tracking runs first.
 */
@Suppress("LongParameterList")
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackRewardedAd(
    context: Context,
    adUnitId: String,
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: RewardedAdLoadCallback? = null,
    fullScreenContentCallback: FullScreenContentCallback? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    RewardedAd.load(
        context,
        adUnitId,
        adRequest,
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                trackIfConfigured {
                    adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = ad.responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.REWARDED,
                            placement = placement,
                            adUnitId = adUnitId,
                            impressionId = ad.responseInfo.responseId.orEmpty(),
                        ),
                    )
                }
                ad.fullScreenContentCallback = TrackingFullScreenContentCallback(
                    delegate = fullScreenContentCallback,
                    adFormat = AdFormat.REWARDED,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                )
                setUpPaidEventTracking(
                    setListener = { ad.onPaidEventListener = it },
                    adFormat = AdFormat.REWARDED,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                    delegate = onPaidEventListener,
                )
                loadCallback?.onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                trackIfConfigured {
                    adTracker.trackAdFailedToLoad(
                        AdFailedToLoadData(
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.REWARDED,
                            placement = placement,
                            adUnitId = adUnitId,
                            mediatorErrorCode = error.code,
                        ),
                    )
                }
                loadCallback?.onAdFailedToLoad(error)
            }
        },
    )
}

/**
 * Loads a [RewardedInterstitialAd] and automatically tracks RevenueCat ad events.
 *
 * Call via [Purchases.sharedInstance.adTracker].
 *
 * @param context The context used to load the ad.
 * @param adUnitId The AdMob ad unit ID.
 * @param adRequest The [AdRequest] to use.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success/failure events.
 * @param fullScreenContentCallback Optional callback for full-screen ad lifecycle events.
 * @param onPaidEventListener Optional paid-event callback. RevenueCat tracking runs first.
 */
@Suppress("LongParameterList")
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackRewardedInterstitialAd(
    context: Context,
    adUnitId: String,
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: RewardedInterstitialAdLoadCallback? = null,
    fullScreenContentCallback: FullScreenContentCallback? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    RewardedInterstitialAd.load(
        context,
        adUnitId,
        adRequest,
        object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                trackIfConfigured {
                    adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = ad.responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.REWARDED_INTERSTITIAL,
                            placement = placement,
                            adUnitId = adUnitId,
                            impressionId = ad.responseInfo.responseId.orEmpty(),
                        ),
                    )
                }
                ad.fullScreenContentCallback = TrackingFullScreenContentCallback(
                    delegate = fullScreenContentCallback,
                    adFormat = AdFormat.REWARDED_INTERSTITIAL,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                )
                setUpPaidEventTracking(
                    setListener = { ad.onPaidEventListener = it },
                    adFormat = AdFormat.REWARDED_INTERSTITIAL,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = { ad.responseInfo },
                    delegate = onPaidEventListener,
                )
                loadCallback?.onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                trackIfConfigured {
                    adTracker.trackAdFailedToLoad(
                        AdFailedToLoadData(
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.REWARDED_INTERSTITIAL,
                            placement = placement,
                            adUnitId = adUnitId,
                            mediatorErrorCode = error.code,
                        ),
                    )
                }
                loadCallback?.onAdFailedToLoad(error)
            }
        },
    )
}

/**
 * Sets up RevenueCat ad-event tracking for the given [AdView] and loads the ad.
 *
 * Call via [Purchases.sharedInstance.adTracker].
 *
 * For an AdMob-close API without passing the tracker, use [AdView.loadAndTrackAd] instead.
 *
 * @param adView The [AdView] to track and load.
 * @param adRequest The [AdRequest] to use.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param adListener Optional [AdListener] delegate for ad lifecycle callbacks.
 * @param onPaidEventListener Optional paid-event callback. RevenueCat tracking runs first.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackBannerAd(
    adView: AdView,
    adRequest: AdRequest,
    placement: String? = null,
    adListener: AdListener? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    loadAndTrackBannerAdInternal(
        adView = adView,
        adRequest = adRequest,
        placement = placement,
        adListener = adListener,
        onPaidEventListener = onPaidEventListener,
    )
}
