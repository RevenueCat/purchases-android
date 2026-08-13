@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdTracker
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * Builds every format-specific tracking callback around a caller-supplied delegate, keyed by the
 * SDK interface it wraps.
 *
 * Tests assert coverage against this map, so adding a format here forces the new callback to be
 * covered everywhere instead of silently skipping a suite.
 */
internal val trackingEventCallbackFactories: Map<Class<*>, (Any?) -> AdEventCallback> = mapOf(
    BannerAdEventCallback::class.java to { delegate ->
        TrackingBannerAdEventCallback(delegate as BannerAdEventCallback?, "home", "ad-unit", ::stubResponseInfo)
    },
    InterstitialAdEventCallback::class.java to { delegate ->
        TrackingInterstitialAdEventCallback(
            delegate as InterstitialAdEventCallback?,
            "home",
            "ad-unit",
            ::stubResponseInfo,
        )
    },
    AppOpenAdEventCallback::class.java to { delegate ->
        TrackingAppOpenAdEventCallback(delegate as AppOpenAdEventCallback?, "home", "ad-unit", ::stubResponseInfo)
    },
    RewardedAdEventCallback::class.java to { delegate ->
        TrackingRewardedAdEventCallback(delegate as RewardedAdEventCallback?, "home", "ad-unit", ::stubResponseInfo)
    },
    RewardedInterstitialAdEventCallback::class.java to { delegate ->
        TrackingRewardedInterstitialAdEventCallback(
            delegate as RewardedInterstitialAdEventCallback?,
            "home",
            "ad-unit",
            ::stubResponseInfo,
        )
    },
    NativeAdEventCallback::class.java to { delegate ->
        TrackingNativeAdEventCallback(delegate as NativeAdEventCallback?, "home", "ad-unit", ::stubResponseInfo)
    },
)

internal val trackingEventCallbacksBySdkInterface: Map<Class<*>, Class<*>> =
    trackingEventCallbackFactories.mapValues { (_, create) -> create(null).javaClass }

private fun stubResponseInfo(): ResponseInfo = mockk(relaxed = true)

class TrackingEventCallbackContractTest {
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns mockk<AdTracker>(relaxed = true)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
    }

    @Test
    fun `format callbacks override every SDK callback`() {
        trackingEventCallbacksBySdkInterface.forEach { (sdkCallback, trackingCallback) ->
            assertOverridesAllSdkCallbacks(sdkCallback, trackingCallback)
        }
    }

    /**
     * Overriding a callback is not enough: an override with an empty body silently swallows the
     * application's own callback. Tracking also runs before delegation, so a throw on the tracking
     * path would drop the delegate call. Drive every SDK callback for real and assert it lands.
     */
    @Test
    fun `format callbacks forward every SDK callback to the delegate`() {
        trackingEventCallbackFactories.forEach { (sdkCallback, create) ->
            sdkCallback.callbackMethods().forEach { method ->
                val invoked = mutableListOf<String>()
                val delegate = recordingDelegate(sdkCallback, invoked)
                val trackingCallback = create(delegate)

                method.invoke(trackingCallback, *method.defaultArguments())

                assertEquals(
                    "${trackingCallback.javaClass.simpleName}.${method.name} must forward to the delegate",
                    listOf(method.name),
                    invoked,
                )
            }
        }
    }

    @Test
    fun `format callbacks tolerate a null delegate`() {
        trackingEventCallbackFactories.forEach { (sdkCallback, create) ->
            val trackingCallback = create(null)

            sdkCallback.callbackMethods().forEach { method ->
                method.invoke(trackingCallback, *method.defaultArguments())
            }
        }
    }
}

private fun Class<*>.callbackMethods(): List<Method> = methods
    .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
    .filterNot { it.name in setOf("equals", "hashCode", "toString") }

private fun Method.defaultArguments(): Array<Any> = parameterTypes.map { it.defaultArgument() }.toTypedArray()

private fun Class<*>.defaultArgument(): Any = when {
    this == String::class.java -> "value"
    // Relaxed mocks return null for the precisionType enum, which the revenue mapping dereferences.
    this == AdValue::class.java -> AdValue(PrecisionType.PRECISE, 1_000L, "USD")
    else -> mockkClass(kotlin, relaxed = true)
}

private fun recordingDelegate(sdkCallback: Class<*>, invoked: MutableList<String>): Any =
    Proxy.newProxyInstance(sdkCallback.classLoader, arrayOf(sdkCallback)) { _, method, _ ->
        invoked += method.name
        null
    }
