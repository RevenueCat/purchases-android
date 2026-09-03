@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame

internal data class FullScreenAdTestValues(
    val adFormat: AdFormat,
    val adUnitId: String,
    val placement: String,
)

internal class CallbackHolder<CallbackT> {
    var callback: CallbackT? = null

    fun requireCallback(): CallbackT = requireNotNull(callback) { "Expected an ad event callback to be installed" }
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

internal class FullScreenAdResponseFlowContract<AdT, CallbackT : AdEventCallback>(
    private val adTracker: AdTracker,
    private val values: FullScreenAdTestValues,
) {
    @Suppress("LongParameterList")
    fun responseSuccess(
        createAd: (ResponseInfo, CallbackHolder<CallbackT>, () -> Unit) -> AdT,
        eventCallback: CallbackT,
        stubLoadFromResponse: (CallbackHolder<AdLoadCallback<AdT>>) -> Unit,
        loadAndTrackFromResponse: (AdLoadCallback<AdT>, CallbackT) -> Unit,
        asTrackingCallback: (CallbackT) -> TrackingAdEventCallback<CallbackT>?,
        invokeDelegateCallback: (CallbackT) -> Unit,
        assertDelegateInvoked: () -> Unit,
    ) {
        val order = mutableListOf<String>()
        val responseInfo = responseInfo()
        val installedCallback = CallbackHolder<CallbackT>()
        val expectedAd = createAd(responseInfo, installedCallback) { order += "event-callback" }
        val loadCallback = object : AdLoadCallback<AdT> {
            override fun onAdLoaded(ad: AdT) {
                order += "load-callback"
                assertSame(expectedAd, ad)
            }
        }
        val trackingLoadCallback = CallbackHolder<AdLoadCallback<AdT>>()
        val loadedData = slot<AdLoadedData>()

        stubLoadFromResponse(trackingLoadCallback)
        every { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        loadAndTrackFromResponse(loadCallback, eventCallback)
        trackingLoadCallback.requireCallback().onAdLoaded(expectedAd)

        assertEquals(listOf("tracked", "event-callback", "load-callback"), order)
        adTracker.assertLoadedData(
            loadedData = loadedData,
            values = values,
            networkName = "test-network",
            impressionId = "response-id",
        )

        val callback = installedCallback.requireCallback()
        requireNotNull(asTrackingCallback(callback)) { "Expected a format-specific tracking callback" }
        invokeDelegateCallback(callback)
        assertDelegateInvoked()
    }

    fun responseFailure(
        stubLoadFromResponse: (CallbackHolder<AdLoadCallback<AdT>>) -> Unit,
        loadAndTrackFromResponse: (AdLoadCallback<AdT>) -> Unit,
    ) {
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

        stubLoadFromResponse(trackingLoadCallback)
        every { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        loadAndTrackFromResponse(loadCallback)
        trackingLoadCallback.requireCallback().onAdFailedToLoad(error)

        assertEquals(listOf("tracked", "load-callback"), order)
        adTracker.assertFailedData(failedData, values)
    }

    private fun responseInfo(): ResponseInfo = mockk(relaxed = true) {
        every { adapterClassName } returns "test-network"
        every { responseId } returns "response-id"
    }
}

internal class FullScreenAdFlowContract<AdT : Ad, CallbackT : AdEventCallback>(
    private val adTracker: AdTracker,
    private val values: FullScreenAdTestValues,
    private val suspendingValues: FullScreenAdTestValues,
) {
    @Suppress("LongParameterList")
    fun callbackSuccess(
        createAd: (ResponseInfo, CallbackHolder<CallbackT>) -> AdT,
        initialEventCallback: CallbackT,
        replacementEventCallback: CallbackT,
        stubLoad: (AdRequest, CallbackHolder<AdLoadCallback<AdT>>) -> Unit,
        loadAndTrack: (AdRequest, AdLoadCallback<AdT>, CallbackT) -> Unit,
        asTrackingCallback: (CallbackT) -> TrackingAdEventCallback<CallbackT>?,
        invokeDelegateCallback: (CallbackT) -> Unit,
        assertInitialDelegateInvoked: () -> Unit,
        setTrackingEventCallback: (AdT, CallbackT) -> Unit,
        assertReplacementDelegateInvoked: () -> Unit,
        show: (AdT, Activity, String) -> Unit,
        verifyShow: (AdT, Activity) -> Unit,
    ) {
        val adRequest = adRequest(values.adUnitId)
        val installedCallback = CallbackHolder<CallbackT>()
        val ad = createAd(responseInfo("test-network", "response-id"), installedCallback)
        val activity = mockk<Activity>()
        val loadCallback = FullScreenRecordingAdLoadCallback<AdT>()
        val trackingLoadCallback = CallbackHolder<AdLoadCallback<AdT>>()

        stubLoad(adRequest, trackingLoadCallback)
        loadAndTrack(adRequest, loadCallback, initialEventCallback)
        trackingLoadCallback.requireCallback().onAdLoaded(ad)

        assertSame(ad, loadCallback.loadedAd)
        val loadedData = slot<AdLoadedData>()
        adTracker.assertLoadedData(loadedData, values, "test-network", "response-id")

        val callback = installedCallback.requireCallback()
        val trackingCallback = requireNotNull(asTrackingCallback(callback)) {
            "Expected a format-specific tracking callback"
        }
        invokeDelegateCallback(callback)
        assertInitialDelegateInvoked()

        setTrackingEventCallback(ad, replacementEventCallback)
        invokeDelegateCallback(callback)
        assertReplacementDelegateInvoked()

        show(ad, activity, "show-placement")
        assertEquals("show-placement", trackingCallback.placement)
        verifyShow(ad, activity)
    }

    fun callbackFailure(
        stubLoad: (AdRequest, CallbackHolder<AdLoadCallback<AdT>>) -> Unit,
        loadAndTrack: (AdRequest, AdLoadCallback<AdT>) -> Unit,
    ) {
        val adRequest = adRequest(values.adUnitId)
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = FullScreenRecordingAdLoadCallback<AdT>()
        val trackingLoadCallback = CallbackHolder<AdLoadCallback<AdT>>()

        stubLoad(adRequest, trackingLoadCallback)
        loadAndTrack(adRequest, loadCallback)
        trackingLoadCallback.requireCallback().onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
        adTracker.assertFailedData(slot(), values)
    }

    @Suppress("LongParameterList")
    suspend fun suspendingSuccess(
        createAd: (ResponseInfo, CallbackHolder<CallbackT>) -> AdT,
        eventCallback: CallbackT,
        stubLoad: (AdRequest, AdLoadResult<AdT>) -> Unit,
        loadAndTrack: suspend (AdRequest, CallbackT) -> AdLoadResult<AdT>,
        asTrackingCallback: (CallbackT) -> TrackingAdEventCallback<CallbackT>?,
        invokeDelegateCallback: (CallbackT) -> Unit,
        assertDelegateInvoked: () -> Unit,
    ) {
        val adRequest = adRequest(suspendingValues.adUnitId)
        val installedCallback = CallbackHolder<CallbackT>()
        val ad = createAd(responseInfo("suspend-test-network", "suspend-response-id"), installedCallback)
        val sdkResult = AdLoadResult.Success(ad)

        stubLoad(adRequest, sdkResult)
        val result = loadAndTrack(adRequest, eventCallback)

        assertSame(sdkResult, result)
        val callback = installedCallback.requireCallback()
        requireNotNull(asTrackingCallback(callback)) { "Expected a format-specific tracking callback" }
        invokeDelegateCallback(callback)
        assertDelegateInvoked()

        val loadedData = slot<AdLoadedData>()
        adTracker.assertLoadedData(
            loadedData,
            suspendingValues,
            networkName = "suspend-test-network",
            impressionId = "suspend-response-id",
        )
    }

    suspend fun suspendingFailure(
        stubLoad: (AdRequest, AdLoadResult<AdT>) -> Unit,
        loadAndTrack: suspend (AdRequest) -> AdLoadResult<AdT>,
    ) {
        val adRequest = adRequest(suspendingValues.adUnitId)
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR
        }
        val sdkResult = AdLoadResult.Failure<AdT>(error)

        stubLoad(adRequest, sdkResult)
        val result = loadAndTrack(adRequest)

        assertSame(sdkResult, result)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = suspendingValues.adFormat,
                placement = suspendingValues.placement,
                adUnitId = suspendingValues.adUnitId,
                mediatorErrorCode = LoadAdError.ErrorCode.NETWORK_ERROR.value,
            ),
            failedData.captured,
        )
    }

    fun eventCallbackFallback(
        createAd: () -> AdT,
        eventCallback: CallbackT,
        setTrackingEventCallback: (AdT, CallbackT) -> Unit,
        verifyEventCallbackInstalled: (AdT, CallbackT) -> Unit,
    ) {
        val ad = createAd()

        setTrackingEventCallback(ad, eventCallback)

        verifyEventCallbackInstalled(ad, eventCallback)
    }

    private fun adRequest(adUnitId: String): AdRequest = mockk {
        every { this@mockk.adUnitId } returns adUnitId
    }

    private fun responseInfo(networkName: String, impressionId: String): ResponseInfo = mockk(relaxed = true) {
        every { adapterClassName } returns networkName
        every { responseId } returns impressionId
    }
}

internal fun <CallbackT : AdEventCallback> assertShowClearsPlacement(
    trackingCallback: TrackingAdEventCallback<CallbackT>,
    show: () -> Unit,
    verifyShow: () -> Unit,
) {
    assertEquals("load-placement", trackingCallback.placement)

    show()

    assertNull(trackingCallback.placement)
    verifyShow()
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
