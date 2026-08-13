package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import org.junit.Test

/**
 * Every format-specific tracking callback, keyed by the SDK interface it wraps.
 *
 * Tests assert coverage against this map, so adding a format here forces the new callback to be
 * covered everywhere instead of silently skipping a suite.
 */
internal val trackingEventCallbacksBySdkInterface: Map<Class<*>, Class<*>> = mapOf(
    BannerAdEventCallback::class.java to TrackingBannerAdEventCallback::class.java,
    InterstitialAdEventCallback::class.java to TrackingInterstitialAdEventCallback::class.java,
    AppOpenAdEventCallback::class.java to TrackingAppOpenAdEventCallback::class.java,
    RewardedAdEventCallback::class.java to TrackingRewardedAdEventCallback::class.java,
    RewardedInterstitialAdEventCallback::class.java to TrackingRewardedInterstitialAdEventCallback::class.java,
    NativeAdEventCallback::class.java to TrackingNativeAdEventCallback::class.java,
)

class TrackingEventCallbackContractTest {

    @Test
    fun `format callbacks override every SDK callback`() {
        trackingEventCallbacksBySdkInterface.forEach { (sdkCallback, trackingCallback) ->
            assertOverridesAllSdkCallbacks(sdkCallback, trackingCallback)
        }
    }
}
