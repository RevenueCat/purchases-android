package com.revenuecat.apitester.kotlin

import android.content.Context
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.forNativeAdWithTracking
import com.revenuecat.purchases.admob.loadAndTrackAd
import com.revenuecat.purchases.admob.loadAndTrackAppOpenAd
import com.revenuecat.purchases.admob.loadAndTrackBannerAd
import com.revenuecat.purchases.admob.loadAndTrackInterstitialAd
import com.revenuecat.purchases.admob.loadAndTrackRewardedAd
import com.revenuecat.purchases.admob.loadAndTrackRewardedInterstitialAd

@Suppress("unused", "UNUSED_VARIABLE")
private class AdMobAPI {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun check(
        context: Context,
        adView: AdView,
        adRequest: AdRequest,
        adUnitId: String,
        nativeAdOptions: NativeAdOptions,
    ) {
        val adTracker = Purchases.sharedInstance.adTracker

        // AdView extension API (AdMob-close)
        adView.loadAndTrackAd(adRequest = adRequest, placement = "api_tester_banner")

        // AdTracker load-and-track APIs
        adTracker.loadAndTrackBannerAd(adView = adView, adRequest = adRequest, placement = "api_tester_banner")
        adTracker.loadAndTrackInterstitialAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        adTracker.loadAndTrackAppOpenAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        adTracker.loadAndTrackRewardedAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        adTracker.loadAndTrackRewardedInterstitialAd(context = context, adUnitId = adUnitId, adRequest = adRequest)

        // Native builder extension API
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAdWithTracking(adUnitId = adUnitId) {}
            .withNativeAdOptions(nativeAdOptions)
            .build()
        adLoader.loadAd(adRequest)
    }
}
