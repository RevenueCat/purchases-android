@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdEventCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame

internal data class FullScreenAdTestValues(
    val adFormat: AdFormat,
    val adUnitId: String,
    val placement: String,
)

internal class CallbackHolder<CallbackT> {
    var callback: CallbackT? = null

    fun requireCallback(): CallbackT = requireNotNull(callback) { "Expected an ad callback to be installed" }
}

internal class FullScreenRecordingAdLoadCallback<AdT> : AdLoadCallback<AdT> {
    var loadedAd: AdT? = null
    var loadError: LoadAdError? = null

    override fun onAdLoaded(ad: AdT) {
        loadedAd = ad
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        loadError = adError
    }
}

/** Format-specific, statically typed wiring exercised by the shared response-flow contract. */
internal interface FullScreenAdResponseFlowAdapter<AdT, CallbackT : AdEventCallback> {
    val values: FullScreenAdTestValues

    fun createAd(
        responseInfo: ResponseInfo,
        installedCallback: CallbackHolder<CallbackT>,
        onCallbackInstalled: () -> Unit,
    ): AdT

    fun stubLoadFromResponse(trackingLoadCallback: CallbackHolder<AdLoadCallback<AdT>>)

    fun loadAndTrackFromResponse(
        loadCallback: AdLoadCallback<AdT>,
        eventCallback: CallbackT?,
    )

    fun asTrackingCallback(callback: CallbackT): TrackingAdEventCallback<CallbackT>?

    fun invokeDelegateCallback(callback: CallbackT)
}

internal class FullScreenAdResponseFlowContract<AdT, CallbackT : AdEventCallback>(
    private val adTracker: AdTracker,
    private val adapter: FullScreenAdResponseFlowAdapter<AdT, CallbackT>,
) {
    fun responseSuccess(
        eventCallback: CallbackT,
        assertDelegateInvoked: () -> Unit,
    ) {
        val order = mutableListOf<String>()
        val installedCallback = CallbackHolder<CallbackT>()
        val expectedAd = adapter.createAd(responseInfo(), installedCallback) { order += "event-callback" }
        val loadCallback = object : AdLoadCallback<AdT> {
            override fun onAdLoaded(ad: AdT) {
                order += "load-callback"
                assertSame(expectedAd, ad)
            }
        }
        val trackingLoadCallback = CallbackHolder<AdLoadCallback<AdT>>()
        val loadedData = slot<AdLoadedData>()

        adapter.stubLoadFromResponse(trackingLoadCallback)
        every { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        adapter.loadAndTrackFromResponse(loadCallback, eventCallback)
        trackingLoadCallback.requireCallback().onAdLoaded(expectedAd)

        assertEquals(listOf("tracked", "event-callback", "load-callback"), order)
        adTracker.assertLoadedData(
            loadedData = loadedData,
            values = adapter.values,
            networkName = "test-network",
            impressionId = "response-id",
        )

        val callback = installedCallback.requireCallback()
        requireNotNull(adapter.asTrackingCallback(callback)) { "Expected a format-specific tracking callback" }
        adapter.invokeDelegateCallback(callback)
        assertDelegateInvoked()
    }

    fun responseFailure() {
        val order = mutableListOf<String>()
        val error = LoadAdError(
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE,
            "invalid response",
            mockk(relaxed = true),
        )
        val loadCallback = object : AdLoadCallback<AdT> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                order += "load-callback"
                assertSame(error, adError)
            }
        }
        val trackingLoadCallback = CallbackHolder<AdLoadCallback<AdT>>()
        val failedData = slot<AdFailedToLoadData>()

        adapter.stubLoadFromResponse(trackingLoadCallback)
        every { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        adapter.loadAndTrackFromResponse(loadCallback, eventCallback = null)
        trackingLoadCallback.requireCallback().onAdFailedToLoad(error)

        assertEquals(listOf("tracked", "load-callback"), order)
        adTracker.assertFailedData(failedData, adapter.values)
    }

    private fun responseInfo(): ResponseInfo = mockk(relaxed = true) {
        every { adapterClassName } returns "test-network"
        every { responseId } returns "response-id"
    }
}

internal fun AdTracker.assertLoadedData(
    loadedData: CapturingSlot<AdLoadedData>,
    values: FullScreenAdTestValues,
    networkName: String,
    impressionId: String,
) {
    verify(exactly = 1) {
        trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
    }
    assertEquals(
        AdLoadedData(
            networkName = networkName,
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = values.adFormat,
            placement = values.placement,
            adUnitId = values.adUnitId,
            impressionId = impressionId,
        ),
        loadedData.captured,
    )
}

internal fun AdTracker.assertFailedData(
    failedData: CapturingSlot<AdFailedToLoadData>,
    values: FullScreenAdTestValues,
) {
    verify(exactly = 1) {
        trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
    }
    assertEquals(values.adFormat, failedData.captured.adFormat)
    assertEquals(values.adUnitId, failedData.captured.adUnitId)
    assertEquals(values.placement, failedData.captured.placement)
}

internal fun AdTracker.assertSuspendingFailedData(
    failedData: CapturingSlot<AdFailedToLoadData>,
    values: FullScreenAdTestValues,
) {
    verify(exactly = 1) {
        trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
    }
    assertEquals(
        AdFailedToLoadData(
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = values.adFormat,
            placement = values.placement,
            adUnitId = values.adUnitId,
            mediatorErrorCode = LoadAdError.ErrorCode.NETWORK_ERROR.value,
        ),
        failedData.captured,
    )
}
