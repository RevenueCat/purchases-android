@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.mockk

/**
 * One format-specific tracking callback, paired with the [AdFormat] and [AdDisplayedTrigger] it is
 * expected to report.
 *
 * The format callbacks differ only in what they hand to the base class, so a copy-paste slip is the
 * likely failure and every suite needs to drive all six. Keeping the list here rather than beside
 * one suite is what makes adding a format force coverage everywhere instead of silently skipping
 * whichever suite kept its own list.
 */
internal class TrackingEventCallbackFixture(
    val sdkCallback: Class<*>,
    val adFormat: AdFormat,
    val displayTrigger: AdDisplayedTrigger,
    val create: (delegate: Any?, responseInfoProvider: () -> ResponseInfo) -> AdEventCallback,
) {
    val description: String get() = sdkCallback.simpleName
}

internal val trackingEventCallbackFixtures = listOf(
    TrackingEventCallbackFixture(
        BannerAdEventCallback::class.java,
        AdFormat.BANNER,
        AdDisplayedTrigger.IMPRESSION,
    ) { delegate, responseInfoProvider ->
        TrackingBannerAdEventCallback(delegate as BannerAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        NativeAdEventCallback::class.java,
        AdFormat.NATIVE,
        AdDisplayedTrigger.IMPRESSION,
    ) { delegate, responseInfoProvider ->
        TrackingNativeAdEventCallback(delegate as NativeAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        InterstitialAdEventCallback::class.java,
        AdFormat.INTERSTITIAL,
        AdDisplayedTrigger.FULL_SCREEN_SHOW,
    ) { delegate, responseInfoProvider ->
        TrackingInterstitialAdEventCallback(
            delegate as InterstitialAdEventCallback?,
            "home",
            "ad-unit",
            responseInfoProvider,
        )
    },
    TrackingEventCallbackFixture(
        AppOpenAdEventCallback::class.java,
        AdFormat.APP_OPEN,
        AdDisplayedTrigger.FULL_SCREEN_SHOW,
    ) { delegate, responseInfoProvider ->
        TrackingAppOpenAdEventCallback(delegate as AppOpenAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        RewardedAdEventCallback::class.java,
        AdFormat.REWARDED,
        AdDisplayedTrigger.FULL_SCREEN_SHOW,
    ) { delegate, responseInfoProvider ->
        TrackingRewardedAdEventCallback(delegate as RewardedAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        RewardedInterstitialAdEventCallback::class.java,
        AdFormat.REWARDED_INTERSTITIAL,
        AdDisplayedTrigger.FULL_SCREEN_SHOW,
    ) { delegate, responseInfoProvider ->
        TrackingRewardedInterstitialAdEventCallback(
            delegate as RewardedInterstitialAdEventCallback?,
            "home",
            "ad-unit",
            responseInfoProvider,
        )
    },
)

internal fun stubResponseInfo(): ResponseInfo = mockk(relaxed = true)
