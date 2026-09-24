@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.PreloaderTest
import com.revenuecat.purchases.admob.nextgen.responseInfo
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.confirmVerified
import io.mockk.every
import org.junit.Assert.assertEquals
import org.junit.Test

internal class TrackingPreloadCallbackTest : PreloaderTest() {

    @Test
    fun `tracks a successful preload before forwarding it unchanged`() {
        val order = mutableListOf<String>()
        every { adTracker.trackAdLoaded(any(), any()) } answers { order += "track" }
        val delegate = RecordingPreloadCallback(
            onPreloaded = { preloadId -> order += "delegate:$preloadId" },
        )
        val callback = trackingCallback(delegate)

        assertSuccessfulPreload(
            preloadId = PRELOAD_ID,
            trackingCallback = callback,
            delegate = delegate,
            expectedFormat = AdFormat.NATIVE,
            expectedAdUnitId = AD_UNIT_ID,
            expectedPlacement = PLACEMENT,
        )

        assertEquals(listOf("track", "delegate:$PRELOAD_ID"), order)
    }

    @Test
    fun `tracks a failed preload before forwarding it unchanged`() {
        val order = mutableListOf<String>()
        every { adTracker.trackAdFailedToLoad(any(), any()) } answers { order += "track" }
        val delegate = RecordingPreloadCallback(
            onFailedToPreload = { preloadId -> order += "delegate:$preloadId" },
        )
        val callback = trackingCallback(delegate)

        assertFailedPreload(
            preloadId = PRELOAD_ID,
            trackingCallback = callback,
            delegate = delegate,
            expectedFormat = AdFormat.NATIVE,
            expectedAdUnitId = AD_UNIT_ID,
            expectedPlacement = PLACEMENT,
        )

        assertEquals(listOf("track", "delegate:$PRELOAD_ID"), order)
    }

    @Test
    fun `forwards exhaustion without tracking it`() {
        val delegate = RecordingPreloadCallback()

        trackingCallback(delegate).onAdsExhausted(PRELOAD_ID)

        assertEquals(listOf(PRELOAD_ID), delegate.exhaustedIds)
        confirmVerified(adTracker)
    }

    @Test
    fun `tolerates a null delegate for every callback`() {
        val callback = trackingCallback(delegate = null)

        callback.onAdPreloaded(PRELOAD_ID, responseInfo("test-network", "test-response"))
        callback.onAdFailedToPreload(PRELOAD_ID, loadError(LoadAdError.ErrorCode.NO_FILL))
        callback.onAdsExhausted(PRELOAD_ID)
    }

    private fun trackingCallback(delegate: PreloadCallback?): TrackingPreloadCallback = TrackingPreloadCallback(
        delegate = delegate,
        adFormat = AdFormat.NATIVE,
        placement = PLACEMENT,
        adUnitId = AD_UNIT_ID,
    )

    companion object {
        private const val PRELOAD_ID = "test-buffer"
        private const val PLACEMENT = "test-placement"
        private const val AD_UNIT_ID = "test-unit"
    }
}
