@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

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
        trackingEventCallbackFixtures.forEach { fixture ->
            assertOverridesAllSdkCallbacks(
                fixture.sdkCallback,
                fixture.create(null, ::stubResponseInfo).javaClass,
            )
        }
    }

    /**
     * Overriding a callback is not enough: an override with an empty body silently swallows the
     * application's own callback. Tracking also runs before delegation, so a throw on the tracking
     * path would drop the delegate call. Drive every SDK callback for real and assert it lands
     * exactly once with its arguments unchanged, which also rules out a swapped or dropped argument.
     */
    @Test
    fun `format callbacks forward every SDK callback to the delegate`() {
        trackingEventCallbackFixtures.forEach { fixture ->
            fixture.sdkCallback.callbackMethods().forEach { method ->
                val invoked = mutableListOf<Pair<String, List<Any?>>>()
                val delegate = recordingDelegate(fixture.sdkCallback, invoked)
                val trackingCallback = fixture.create(delegate, ::stubResponseInfo)
                val arguments = method.distinctArguments()

                method.invoke(trackingCallback, *arguments)

                assertEquals(
                    "${trackingCallback.javaClass.simpleName}.${method.name} must forward unchanged",
                    listOf(method.name to arguments.toList()),
                    invoked,
                )
            }
        }
    }

    /**
     * The forwarding sweep above only checks that the delegate ran, so a callback that quietly
     * tracked something extra — a second display on dismissal, say — would still pass it. Drive
     * every callback against a fresh tracker, require exactly the event the fixture declares, and
     * close with [confirmVerified] so anything beyond it fails.
     */
    @Test
    fun `each SDK callback tracks exactly the event the fixture declares`() {
        trackingEventCallbackFixtures.forEach { fixture ->
            assertEquals(
                "${fixture.description}: expectedEvents must list every SDK callback, no more and no less",
                fixture.sdkCallback.callbackMethods().map { it.name }.toSet(),
                fixture.expectedEvents.keys,
            )

            fixture.sdkCallback.callbackMethods().forEach { method ->
                val tracker = mockk<AdTracker>(relaxed = true)
                every { purchases.adTracker } returns tracker
                val trackingCallback = fixture.create(null, ::stubResponseInfo)
                val label = "${fixture.description}.${method.name}"

                method.invoke(trackingCallback, *method.distinctArguments())

                when (fixture.expectedEvents.getValue(method.name)) {
                    ExpectedAdEvent.NONE -> Unit
                    ExpectedAdEvent.DISPLAYED -> {
                        val tracked = slot<AdDisplayedData>()
                        verify(exactly = 1) {
                            tracker.trackAdDisplayed(capture(tracked), AdCaptureMethod.ADAPTER)
                        }
                        assertEquals(label, fixture.adFormat, tracked.captured.adFormat)
                    }
                    ExpectedAdEvent.OPENED -> verify(exactly = 1) {
                        tracker.trackAdOpened(any(), AdCaptureMethod.ADAPTER)
                    }
                    ExpectedAdEvent.REVENUE -> verify(exactly = 1) {
                        tracker.trackAdRevenue(any(), AdCaptureMethod.ADAPTER)
                    }
                }

                confirmVerified(tracker)
            }
        }
    }

    @Test
    fun `format callbacks tolerate a null delegate`() {
        trackingEventCallbackFixtures.forEach { fixture ->
            val trackingCallback = fixture.create(null, ::stubResponseInfo)

            fixture.sdkCallback.callbackMethods().forEach { method ->
                method.invoke(trackingCallback, *method.distinctArguments())
            }
        }
    }

    /**
     * The reflective sweep cannot supply nulls, because Java reflection does not expose which SDK
     * parameters are nullable and a null would trip the Kotlin null check on the non-null ones.
     * `onAppEvent` carries the only nullable parameter in the callback surface, so it is covered here.
     */
    @Test
    fun `app event forwards a null payload unchanged`() {
        val seen = mutableListOf<Pair<String, String?>>()
        val callback = TrackingBannerAdEventCallback(
            initialDelegate = object : BannerAdEventCallback {
                override fun onAppEvent(name: String, data: String?) {
                    seen += name to data
                }
            },
            initialPlacement = "home",
            adUnitId = "ad-unit",
            responseInfoProvider = ::stubResponseInfo,
        )

        callback.onAppEvent("event", null)

        assertEquals(listOf("event" to null), seen)
    }
}

private fun Class<*>.callbackMethods(): List<Method> = methods
    .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
    .filterNot { it.name in setOf("equals", "hashCode", "toString") }

/** Distinct per position, so forwarding arguments in the wrong order fails the assertion. */
private fun Method.distinctArguments(): Array<Any> =
    parameterTypes.mapIndexed { index, type -> type.argument(index) }.toTypedArray()

private fun Class<*>.argument(index: Int): Any = when {
    this == String::class.java -> "arg$index"
    // Relaxed mocks return null for the precisionType enum, which the revenue mapping dereferences.
    this == AdValue::class.java -> AdValue(PrecisionType.PRECISE, 1_000L, "USD")
    else -> mockkClass(kotlin, relaxed = true)
}

private fun recordingDelegate(sdkCallback: Class<*>, invoked: MutableList<Pair<String, List<Any?>>>): Any =
    Proxy.newProxyInstance(sdkCallback.classLoader, arrayOf(sdkCallback)) { _, method, arguments ->
        invoked += method.name to (arguments?.toList() ?: emptyList())
        null
    }
