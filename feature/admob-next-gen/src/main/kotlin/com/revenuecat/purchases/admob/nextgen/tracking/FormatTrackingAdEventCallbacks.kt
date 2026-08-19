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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingBannerAdEventCallback(
    initialDelegate: BannerAdEventCallback?,
    initialPlacement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<BannerAdEventCallback>(
    initialDelegate,
    AdFormat.BANNER,
    initialPlacement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.IMPRESSION,
),
    BannerAdEventCallback {
    override fun onAppEvent(name: String, data: String?) {
        delegate?.onAppEvent(name, data)
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingInterstitialAdEventCallback(
    initialDelegate: InterstitialAdEventCallback?,
    initialPlacement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<InterstitialAdEventCallback>(
    initialDelegate,
    AdFormat.INTERSTITIAL,
    initialPlacement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    InterstitialAdEventCallback {
    override fun onAppEvent(name: String, data: String?) {
        delegate?.onAppEvent(name, data)
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingAppOpenAdEventCallback(
    initialDelegate: AppOpenAdEventCallback?,
    initialPlacement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<AppOpenAdEventCallback>(
    initialDelegate,
    AdFormat.APP_OPEN,
    initialPlacement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    AppOpenAdEventCallback

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingRewardedAdEventCallback(
    initialDelegate: RewardedAdEventCallback?,
    initialPlacement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<RewardedAdEventCallback>(
    initialDelegate,
    AdFormat.REWARDED,
    initialPlacement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    RewardedAdEventCallback {
    override fun onAdMetadataChanged() {
        delegate?.onAdMetadataChanged()
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingRewardedInterstitialAdEventCallback(
    initialDelegate: RewardedInterstitialAdEventCallback?,
    initialPlacement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<RewardedInterstitialAdEventCallback>(
    initialDelegate,
    AdFormat.REWARDED_INTERSTITIAL,
    initialPlacement,
    adUnitId,
    responseInfoProvider,
    AdDisplayedTrigger.FULL_SCREEN_SHOW,
),
    RewardedInterstitialAdEventCallback {
    override fun onAdMetadataChanged() {
        delegate?.onAdMetadataChanged()
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class TrackingNativeAdEventCallback(
    initialDelegate: NativeAdEventCallback?,
    initialPlacement: String?,
    adUnitId: String,
    responseInfoProvider: () -> ResponseInfo,
) : TrackingAdEventCallback<NativeAdEventCallback>(
    initialDelegate,
    AdFormat.NATIVE,
    initialPlacement,
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
