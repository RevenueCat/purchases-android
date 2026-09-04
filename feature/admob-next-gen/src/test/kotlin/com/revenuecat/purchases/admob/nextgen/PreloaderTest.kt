@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before

internal abstract class PreloaderTest {

    protected val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUpPurchases() {
        every { purchases.adTracker } returns adTracker
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDownPurchases() {
        unmockkObject(Purchases)
    }

    protected fun assertStartInstallsPreloadTracking(
        expectedAdFormat: AdFormat,
        stubStart: (String, PreloadConfiguration, CapturingSlot<PreloadCallback>) -> Unit,
        startAndTrack: (String, PreloadConfiguration, String?, PreloadCallback) -> Boolean,
    ) {
        val configuration = preloadConfiguration(AD_UNIT_ID)
        val delegate = RecordingPreloadCallback()
        val trackingCallback = slot<PreloadCallback>()
        stubStart(PRELOAD_ID, configuration, trackingCallback)

        val started = startAndTrack(PRELOAD_ID, configuration, START_PLACEMENT, delegate)

        assertTrue(started)
        assertSuccessfulPreload(
            preloadId = PRELOAD_ID,
            trackingCallback = trackingCallback.captured,
            delegate = delegate,
            expectedFormat = expectedAdFormat,
            expectedAdUnitId = AD_UNIT_ID,
            expectedPlacement = START_PLACEMENT,
        )
    }

    protected fun assertNullPollContract(
        stubNullPoll: (String) -> Unit,
        pollAndTrackAd: (String) -> Any?,
    ) {
        stubNullPoll(PRELOAD_ID)

        assertNull(pollAndTrackAd(PRELOAD_ID))
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
    }

    protected fun responseInfo(networkName: String, responseId: String): ResponseInfo = mockk(relaxed = true) {
        every { adapterClassName } returns networkName
        every { this@mockk.responseId } returns responseId
    }

    protected fun loadError(errorCode: LoadAdError.ErrorCode): LoadAdError = mockk {
        every { code } returns errorCode
    }

    protected fun preloadConfiguration(adUnitId: String): PreloadConfiguration {
        val request = mockk<AdRequest> { every { this@mockk.adUnitId } returns adUnitId }
        return PreloadConfiguration(request, 2)
    }

    protected fun assertSuccessfulPreload(
        preloadId: String,
        trackingCallback: PreloadCallback,
        delegate: RecordingPreloadCallback,
        expectedFormat: AdFormat,
        expectedAdUnitId: String,
        expectedPlacement: String?,
    ) {
        val responseInfo = responseInfo("test-network", "test-response")

        trackingCallback.onAdPreloaded(preloadId, responseInfo)

        assertEquals(listOf(responseInfo), delegate.responses)
        val loadedData = slot<AdLoadedData>()
        verify(exactly = 1) { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) }
        assertEquals(expectedFormat, loadedData.captured.adFormat)
        assertEquals(expectedAdUnitId, loadedData.captured.adUnitId)
        assertEquals(expectedPlacement, loadedData.captured.placement)
    }

    protected fun assertFailedPreload(
        preloadId: String,
        trackingCallback: PreloadCallback,
        delegate: RecordingPreloadCallback,
        expectedFormat: AdFormat,
        expectedAdUnitId: String,
        expectedPlacement: String?,
    ) {
        val error = loadError(LoadAdError.ErrorCode.NO_FILL)

        trackingCallback.onAdFailedToPreload(preloadId, error)

        assertEquals(listOf(error), delegate.errors)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) }
        assertEquals(expectedFormat, failedData.captured.adFormat)
        assertEquals(expectedAdUnitId, failedData.captured.adUnitId)
        assertEquals(expectedPlacement, failedData.captured.placement)
        assertEquals(LoadAdError.ErrorCode.NO_FILL.value, failedData.captured.mediatorErrorCode)
    }

    protected class RecordingPreloadCallback(
        private val onPreloaded: (String) -> Unit = {},
        private val onFailedToPreload: (String) -> Unit = {},
        private val onExhausted: (String) -> Unit = {},
    ) : PreloadCallback {
        val responses = mutableListOf<ResponseInfo>()
        val errors = mutableListOf<LoadAdError>()
        val exhaustedIds = mutableListOf<String>()

        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            responses += responseInfo
            onPreloaded(preloadId)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            errors += adError
            onFailedToPreload(preloadId)
        }

        override fun onAdsExhausted(preloadId: String) {
            exhaustedIds += preloadId
            onExhausted(preloadId)
        }
    }

    private companion object {
        const val PRELOAD_ID = "preload-buffer"
        const val AD_UNIT_ID = "preload-unit"
        const val START_PLACEMENT = "preload-start-placement"
    }
}
