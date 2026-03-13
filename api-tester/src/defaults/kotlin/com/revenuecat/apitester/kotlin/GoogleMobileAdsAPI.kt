package com.revenuecat.apitester.kotlin

import android.content.Context
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.google.mobile.ads.forNativeAdWithTracking
import com.revenuecat.purchases.google.mobile.ads.loadAndTrackAd
import com.revenuecat.purchases.google.mobile.ads.loadAndTrackAppOpenAd
import com.revenuecat.purchases.google.mobile.ads.loadAndTrackBannerAd
import com.revenuecat.purchases.google.mobile.ads.loadAndTrackInterstitialAd
import com.revenuecat.purchases.google.mobile.ads.loadAndTrackRewardedAd
import com.revenuecat.purchases.google.mobile.ads.loadAndTrackRewardedInterstitialAd

@Suppress("unused", "UNUSED_VARIABLE")
private class GoogleMobileAdsAPI {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun check(
        context: Context,
        adView: AdView,
        adRequest: AdRequest,
        adUnitId: String,
        nativeAdOptions: NativeAdOptions,
    ) {
        val adTracker = Purchases.sharedInstance.adTracker

        // AdView extension API (Google Mobile Ads–close)
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
