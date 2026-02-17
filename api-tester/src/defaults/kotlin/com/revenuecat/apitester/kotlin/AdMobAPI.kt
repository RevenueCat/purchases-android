package com.revenuecat.apitester.kotlin

import android.content.Context
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RCAdMob
import com.revenuecat.purchases.admob.forNativeAdWithTracking
import com.revenuecat.purchases.admob.loadAndTrackAd

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
        // Top-level extension API
        adView.loadAndTrackAd(adRequest = adRequest, placement = "api_tester_banner")

        // Static RCAdMob APIs
        RCAdMob.loadAndTrackBannerAd(adView = adView, adRequest = adRequest, placement = "api_tester_banner")
        RCAdMob.loadAndTrackInterstitialAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        RCAdMob.loadAndTrackAppOpenAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        RCAdMob.loadAndTrackRewardedAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        RCAdMob.loadAndTrackRewardedInterstitialAd(context = context, adUnitId = adUnitId, adRequest = adRequest)
        RCAdMob.loadAndTrackNativeAd(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            nativeAdOptions = nativeAdOptions,
        )

        // Native builder extension API
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAdWithTracking(adUnitId = adUnitId) {}
            .withNativeAdOptions(nativeAdOptions)
            .build()
        adLoader.loadAd(adRequest)
    }
}
