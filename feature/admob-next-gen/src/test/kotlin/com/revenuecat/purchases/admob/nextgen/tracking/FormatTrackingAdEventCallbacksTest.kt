package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTrackingAdEventCallbacksTest {

    @Test
    fun `banner forwards app events including nullable data`() {
        var receivedEvent: Pair<String, String?>? = null
        val callback = TrackingBannerAdEventCallback(
            delegate = object : BannerAdEventCallback {
                override fun onAppEvent(name: String, data: String?) {
                    receivedEvent = name to data
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            responseInfo = mockk(),
        )

        callback.onAppEvent("event", null)

        assertEquals("event" to null, receivedEvent)
    }

    @Test
    fun `rewarded forwards metadata changes`() {
        var callbackCount = 0
        val callback = TrackingRewardedAdEventCallback(
            delegate = object : RewardedAdEventCallback {
                override fun onAdMetadataChanged() {
                    callbackCount++
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            responseInfo = mockk<ResponseInfo>(),
        )

        callback.onAdMetadataChanged()

        assertEquals(1, callbackCount)
    }

    @Test
    fun `native forwards format-specific callbacks`() {
        val callbacks = mutableListOf<String>()
        val callback = TrackingNativeAdEventCallback(
            delegate = object : NativeAdEventCallback {
                override fun onAdSwipeGestureClicked() {
                    callbacks += "swipe"
                }

                override fun onCustomMuteThisAdReported() {
                    callbacks += "mute"
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            responseInfo = mockk(),
        )

        callback.onAdSwipeGestureClicked()
        callback.onCustomMuteThisAdReported()

        assertEquals(listOf("swipe", "mute"), callbacks)
    }
}
