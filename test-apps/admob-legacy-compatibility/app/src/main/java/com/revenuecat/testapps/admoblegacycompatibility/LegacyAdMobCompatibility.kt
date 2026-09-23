package com.revenuecat.testapps.admoblegacycompatibility

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.enableRewardVerification
import com.revenuecat.purchases.admob.forNativeAdWithTracking
import com.revenuecat.purchases.admob.loadAndTrackAd
import com.revenuecat.purchases.admob.loadAndTrackAppOpenAd
import com.revenuecat.purchases.admob.loadAndTrackBannerAd
import com.revenuecat.purchases.admob.loadAndTrackInterstitialAd
import com.revenuecat.purchases.admob.loadAndTrackRewardedAd
import com.revenuecat.purchases.admob.loadAndTrackRewardedInterstitialAd
import com.revenuecat.purchases.admob.setTrackingFullScreenContentCallback
import com.revenuecat.purchases.admob.show

/**
 * Compile-only calls covering the public legacy AdMob adapter surface against the consumer's AdMob SDK version.
 * These functions are deliberately not invoked at runtime.
 */
@Suppress("LongParameterList", "unused")
internal object LegacyAdMobCompatibility {

    fun loadAds(
        context: Context,
        adView: AdView,
        adRequest: AdRequest,
        adUnitId: String,
        adListener: AdListener,
        appOpenAdLoadCallback: AppOpenAd.AppOpenAdLoadCallback,
        interstitialAdLoadCallback: InterstitialAdLoadCallback,
        rewardedAdLoadCallback: RewardedAdLoadCallback,
        rewardedInterstitialAdLoadCallback: RewardedInterstitialAdLoadCallback,
        fullScreenContentCallback: FullScreenContentCallback,
        onPaidEventListener: OnPaidEventListener,
    ) {
        val adTracker = Purchases.sharedInstance.adTracker

        adView.loadAndTrackAd(
            adRequest = adRequest,
            placement = "compatibility_banner",
            adListener = adListener,
            onPaidEventListener = onPaidEventListener,
        )
        adTracker.loadAndTrackBannerAd(
            adView = adView,
            adRequest = adRequest,
            placement = "compatibility_banner",
            adListener = adListener,
            onPaidEventListener = onPaidEventListener,
        )
        adTracker.loadAndTrackInterstitialAd(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = "compatibility_interstitial",
            loadCallback = interstitialAdLoadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
        adTracker.loadAndTrackAppOpenAd(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = "compatibility_app_open",
            loadCallback = appOpenAdLoadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
        adTracker.loadAndTrackRewardedAd(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = "compatibility_rewarded",
            loadCallback = rewardedAdLoadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
        adTracker.loadAndTrackRewardedInterstitialAd(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = "compatibility_rewarded_interstitial",
            loadCallback = rewardedInterstitialAdLoadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
    }

    fun loadNativeAd(
        context: Context,
        adRequest: AdRequest,
        adUnitId: String,
        nativeAdOptions: NativeAdOptions,
        adListener: AdListener,
        onPaidEventListener: OnPaidEventListener,
    ) {
        AdLoader.Builder(context, adUnitId)
            .forNativeAdWithTracking(
                adUnitId = adUnitId,
                placement = "compatibility_native",
                adListener = adListener,
                onPaidEventListener = onPaidEventListener,
            ) {}
            .withNativeAdOptions(nativeAdOptions)
            .build()
            .loadAd(adRequest)
    }

    fun configureLoadedAds(
        activity: Activity,
        appOpenAd: AppOpenAd,
        interstitialAd: InterstitialAd,
        rewardedAd: RewardedAd,
        rewardedInterstitialAd: RewardedInterstitialAd,
        callback: FullScreenContentCallback,
        rewardListener: OnUserEarnedRewardListener,
    ) {
        appOpenAd.setTrackingFullScreenContentCallback(callback)
        interstitialAd.setTrackingFullScreenContentCallback(callback)
        rewardedAd.setTrackingFullScreenContentCallback(callback)
        rewardedInterstitialAd.setTrackingFullScreenContentCallback(callback)

        appOpenAd.show(activity, "compatibility_app_open_show")
        interstitialAd.show(activity, "compatibility_interstitial_show")
        rewardedAd.show(activity, "compatibility_rewarded_show", rewardListener)
        rewardedInterstitialAd.show(activity, "compatibility_rewarded_interstitial_show", rewardListener)
    }

    fun verifyRewards(
        activity: Activity,
        rewardedAd: RewardedAd,
        rewardedInterstitialAd: RewardedInterstitialAd,
    ) {
        rewardedAd.enableRewardVerification()
        rewardedInterstitialAd.enableRewardVerification()

        rewardedAd.show(
            activity = activity,
            placement = "compatibility_rewarded_verification",
            rewardVerificationStarted = {},
            rewardVerificationCompleted = {},
        )
        rewardedInterstitialAd.show(
            activity = activity,
            placement = "compatibility_rewarded_interstitial_verification",
            rewardVerificationStarted = {},
            rewardVerificationCompleted = {},
        )
    }
}
