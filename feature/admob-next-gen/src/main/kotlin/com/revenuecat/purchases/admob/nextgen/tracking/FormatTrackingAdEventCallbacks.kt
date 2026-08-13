@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat

internal class TrackingBannerAdEventCallback(
    delegate: BannerAdEventCallback?,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<BannerAdEventCallback>(
    delegate,
    AdFormat.BANNER,
    placement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.IMPRESSION,
),
    BannerAdEventCallback {
    override fun onAppEvent(name: String, data: String?) {
        delegate?.onAppEvent(name, data)
    }
}

internal class TrackingInterstitialAdEventCallback(
    delegate: InterstitialAdEventCallback?,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<InterstitialAdEventCallback>(
    delegate,
    AdFormat.INTERSTITIAL,
    placement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    InterstitialAdEventCallback {
    override fun onAppEvent(name: String, data: String?) {
        delegate?.onAppEvent(name, data)
    }
}

internal class TrackingAppOpenAdEventCallback(
    delegate: AppOpenAdEventCallback?,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<AppOpenAdEventCallback>(
    delegate,
    AdFormat.APP_OPEN,
    placement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    AppOpenAdEventCallback

internal class TrackingRewardedAdEventCallback(
    delegate: RewardedAdEventCallback?,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<RewardedAdEventCallback>(
    delegate,
    AdFormat.REWARDED,
    placement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    RewardedAdEventCallback {
    override fun onAdMetadataChanged() {
        delegate?.onAdMetadataChanged()
    }
}

internal class TrackingRewardedInterstitialAdEventCallback(
    delegate: RewardedInterstitialAdEventCallback?,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<RewardedInterstitialAdEventCallback>(
    delegate,
    AdFormat.REWARDED_INTERSTITIAL,
    placement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    RewardedInterstitialAdEventCallback {
    override fun onAdMetadataChanged() {
        delegate?.onAdMetadataChanged()
    }
}

internal class TrackingNativeAdEventCallback(
    delegate: NativeAdEventCallback?,
    placement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<NativeAdEventCallback>(
    delegate,
    AdFormat.NATIVE,
    placement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.IMPRESSION,
),
    NativeAdEventCallback {
    override fun onAdSwipeGestureClicked() {
        delegate?.onAdSwipeGestureClicked()
    }

    override fun onCustomMuteThisAdReported() {
        delegate?.onCustomMuteThisAdReported()
    }
}
