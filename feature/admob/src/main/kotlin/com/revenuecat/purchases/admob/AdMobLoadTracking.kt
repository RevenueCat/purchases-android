@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
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
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName

internal fun loadAndTrackInterstitialAdInternal(
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

internal fun loadAndTrackAppOpenAdInternal(
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

internal fun loadAndTrackRewardedAdInternal(
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

internal fun loadAndTrackRewardedInterstitialAdInternal(
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
