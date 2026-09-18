
package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.mockk

/** The RevenueCat event an SDK callback must produce, or [NONE] if the adapter does not track it. */
internal enum class ExpectedAdEvent {
    NONE,
    DISPLAYED,
    OPENED,
    REVENUE,
}

/**
 * One format-specific tracking callback, paired with the [AdFormat] it reports and the event every
 * one of its SDK callbacks must produce.
 *
 * [expectedEvents] spells the mapping out rather than deriving it, so the test states the intended
 * behaviour independently of the code under test. The sweep asserts it lists the SDK's callback
 * surface exactly, which is what makes an SDK bump that adds a callback fail here until someone
 * decides whether it should be tracked.
 */
internal class TrackingEventCallbackFixture(
    val sdkCallback: Class<*>,
    val adFormat: AdFormat,
    val expectedEvents: Map<String, ExpectedAdEvent>,
    val create: (delegate: Any?, responseInfoProvider: () -> ResponseInfo) -> AdEventCallback,
) {
    val description: String get() = sdkCallback.simpleName
}

/** Banner and native record the display when the impression lands. */
private val impressionDisplayEvents = mapOf(
    "onAdImpression" to ExpectedAdEvent.DISPLAYED,
    "onAdShowedFullScreenContent" to ExpectedAdEvent.NONE,
    "onAdDismissedFullScreenContent" to ExpectedAdEvent.NONE,
    "onAdFailedToShowFullScreenContent" to ExpectedAdEvent.NONE,
    "onAdClicked" to ExpectedAdEvent.OPENED,
    "onAdPaid" to ExpectedAdEvent.REVENUE,
)

/**
 * Full-screen formats emit the impression too, so they record the display from the show callback
 * and ignore the impression. Tracking both would count one display twice.
 */
private val fullScreenDisplayEvents = mapOf(
    "onAdShowedFullScreenContent" to ExpectedAdEvent.DISPLAYED,
    "onAdImpression" to ExpectedAdEvent.NONE,
    "onAdDismissedFullScreenContent" to ExpectedAdEvent.NONE,
    "onAdFailedToShowFullScreenContent" to ExpectedAdEvent.NONE,
    "onAdClicked" to ExpectedAdEvent.OPENED,
    "onAdPaid" to ExpectedAdEvent.REVENUE,
)

internal val trackingEventCallbackFixtures = listOf(
    TrackingEventCallbackFixture(
        BannerAdEventCallback::class.java,
        AdFormat.BANNER,
        impressionDisplayEvents + mapOf("onAppEvent" to ExpectedAdEvent.NONE),
    ) { delegate, responseInfoProvider ->
        TrackingBannerAdEventCallback(delegate as BannerAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        NativeAdEventCallback::class.java,
        AdFormat.NATIVE,
        impressionDisplayEvents + mapOf(
            "onAdSwipeGestureClicked" to ExpectedAdEvent.NONE,
            "onCustomMuteThisAdReported" to ExpectedAdEvent.NONE,
        ),
    ) { delegate, responseInfoProvider ->
        TrackingNativeAdEventCallback(delegate as NativeAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        InterstitialAdEventCallback::class.java,
        AdFormat.INTERSTITIAL,
        fullScreenDisplayEvents + mapOf("onAppEvent" to ExpectedAdEvent.NONE),
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
        fullScreenDisplayEvents,
    ) { delegate, responseInfoProvider ->
        TrackingAppOpenAdEventCallback(delegate as AppOpenAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        RewardedAdEventCallback::class.java,
        AdFormat.REWARDED,
        fullScreenDisplayEvents + mapOf("onAdMetadataChanged" to ExpectedAdEvent.NONE),
    ) { delegate, responseInfoProvider ->
        TrackingRewardedAdEventCallback(delegate as RewardedAdEventCallback?, "home", "ad-unit", responseInfoProvider)
    },
    TrackingEventCallbackFixture(
        RewardedInterstitialAdEventCallback::class.java,
        AdFormat.REWARDED_INTERSTITIAL,
        fullScreenDisplayEvents + mapOf("onAdMetadataChanged" to ExpectedAdEvent.NONE),
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
