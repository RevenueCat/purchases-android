package com.revenuecat.sample.admob.nextgen.ui

import android.app.Activity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.loadAndTrackAppOpenAd
import com.revenuecat.purchases.admob.nextgen.loadAndTrackInterstitialAd
import com.revenuecat.purchases.admob.nextgen.pollAndTrackAd
import com.revenuecat.purchases.admob.nextgen.show
import com.revenuecat.purchases.admob.nextgen.startAndTrack
import com.revenuecat.sample.admob.nextgen.BuildConfig
import kotlinx.coroutines.launch

private const val INTERSTITIAL_PRELOAD_ID = "sample-interstitial"
private const val APP_OPEN_PRELOAD_ID = "sample-app-open"

@Composable
internal fun InterstitialScreen(activity: Activity, onBack: () -> Unit) = FullScreenAdScreen(
    title = "Interstitial",
    description = "Load placement and show placement are deliberately different to demonstrate show-time overrides.",
    adName = "interstitial",
    preloadId = INTERSTITIAL_PRELOAD_ID,
    onBack = onBack,
    getConfiguration = { InterstitialAdPreloader.getConfiguration(INTERSTITIAL_PRELOAD_ID) },
    getNumAdsAvailable = { InterstitialAdPreloader.getNumAdsAvailable(INTERSTITIAL_PRELOAD_ID) },
    createEventCallback = { onDismissed, onFailedToShow ->
        object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() = onDismissed()

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                onFailedToShow(error)
            }
        }
    },
    loadAd = { callback ->
        Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
            AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID).build(),
            placement = "interstitial_load",
            adEventCallback = callback,
        )
    },
    pollAd = { callback ->
        InterstitialAdPreloader.pollAndTrackAd(
            INTERSTITIAL_PRELOAD_ID,
            placement = "interstitial_poll",
            adEventCallback = callback,
        )
    },
    startPreloader = { bufferSize, callback ->
        InterstitialAdPreloader.startAndTrack(
            preloadId = INTERSTITIAL_PRELOAD_ID,
            preloadConfiguration = PreloadConfiguration(
                AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID).build(),
                bufferSize,
            ),
            placement = "interstitial_preload",
            preloadCallback = callback,
        )
    },
    stopPreloader = { InterstitialAdPreloader.destroy(INTERSTITIAL_PRELOAD_ID) },
    showAd = { ad -> ad.show(activity, placement = "interstitial_show") },
)

@Composable
internal fun AppOpenScreen(activity: Activity, onBack: () -> Unit) = FullScreenAdScreen(
    title = "App open",
    description = "The sample exposes app-open loading explicitly instead of tying it to process lifecycle.",
    adName = "app-open ad",
    preloadId = APP_OPEN_PRELOAD_ID,
    onBack = onBack,
    getConfiguration = { AppOpenAdPreloader.getConfiguration(APP_OPEN_PRELOAD_ID) },
    getNumAdsAvailable = { AppOpenAdPreloader.getNumAdsAvailable(APP_OPEN_PRELOAD_ID) },
    createEventCallback = { onDismissed, onFailedToShow ->
        object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() = onDismissed()

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                onFailedToShow(error)
            }
        }
    },
    loadAd = { callback ->
        Purchases.sharedInstance.adTracker.loadAndTrackAppOpenAd(
            AdRequest.Builder(BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID).build(),
            placement = "app_open_load",
            adEventCallback = callback,
        )
    },
    pollAd = { callback ->
        AppOpenAdPreloader.pollAndTrackAd(
            APP_OPEN_PRELOAD_ID,
            placement = "app_open_poll",
            adEventCallback = callback,
        )
    },
    startPreloader = { bufferSize, callback ->
        AppOpenAdPreloader.startAndTrack(
            APP_OPEN_PRELOAD_ID,
            PreloadConfiguration(
                AdRequest.Builder(BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID).build(),
                bufferSize,
            ),
            placement = "app_open_preload",
            preloadCallback = callback,
        )
    },
    stopPreloader = { AppOpenAdPreloader.destroy(APP_OPEN_PRELOAD_ID) },
    showAd = { ad -> ad.show(activity, placement = "app_open_show") },
)

@Composable
@Suppress("LongMethod", "LongParameterList")
private fun <AdT : Ad, EventCallbackT> FullScreenAdScreen(
    title: String,
    description: String,
    adName: String,
    preloadId: String,
    onBack: () -> Unit,
    getConfiguration: () -> PreloadConfiguration?,
    getNumAdsAvailable: () -> Int,
    createEventCallback: (() -> Unit, (FullScreenContentError) -> Unit) -> EventCallbackT,
    loadAd: suspend (EventCallbackT) -> AdLoadResult<AdT>,
    pollAd: (EventCallbackT) -> AdT?,
    startPreloader: (Int, PreloadCallback) -> Boolean,
    stopPreloader: () -> Boolean,
    showAd: (AdT) -> Unit,
) {
    var directStatus by remember { mutableStateOf("No direct $adName loaded") }
    var directAd by remember { mutableStateOf<AdT?>(null) }
    val preloadState = rememberPreloaderUiState(
        preloadId,
        getConfiguration = getConfiguration,
        getNumAdsAvailable = getNumAdsAvailable,
    )
    val scope = rememberCoroutineScope()
    val directEventCallback = remember {
        createEventCallback(
            {
                scope.launch {
                    directAd = null
                    directStatus = "Direct $adName dismissed"
                }
            },
            { error ->
                scope.launch {
                    directAd = null
                    directStatus = "Show failed: ${error.message}"
                }
            },
        )
    }
    val preloadedEventCallback = remember {
        createEventCallback(
            {
                scope.launch {
                    preloadState.message = "Preloaded $adName dismissed"
                }
            },
            { error ->
                scope.launch {
                    preloadState.message = "Show failed: ${error.message}"
                }
            },
        )
    }

    AdScreen(title, onBack) { mode ->
        Text(description)
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load" to {
                    scope.launch {
                        directStatus = "Loading directly..."
                        when (val result = loadAd(directEventCallback)) {
                            is AdLoadResult.Success -> {
                                directAd = result.ad
                                directStatus = "Direct $adName ready"
                            }
                            is AdLoadResult.Failure -> directStatus = "Load failed: ${result.error.message}"
                        }
                    }
                },
                "Show" to {
                    showFullScreenAd(directAd, adName, showAd) { directStatus = it }
                },
                enabled = mapOf("Show" to (directAd != null)),
            )
        } else {
            PreloaderPanel(
                state = preloadState,
                actions = listOf(
                    PreloaderAction(
                        label = "Poll + Show",
                        enabled = preloadState.started && preloadState.adsAvailable > 0,
                        onClick = {
                            val ad = pollAd(preloadedEventCallback)
                            preloadState.refresh()
                            if (ad == null) {
                                preloadState.message = "No buffered ad available"
                            } else {
                                showFullScreenAd(ad, adName, showAd) { preloadState.message = it }
                            }
                        },
                    ),
                ),
                onToggle = {
                    preloadState.toggle(
                        start = {
                            startPreloader(preloadState.bufferSize, preloadState.preloadCallback(scope))
                        },
                        stop = stopPreloader,
                    )
                },
            )
        }
    }
}

private fun <AdT : Ad> showFullScreenAd(
    ad: AdT?,
    adName: String,
    showAd: (AdT) -> Unit,
    updateStatus: (String) -> Unit,
) {
    if (ad == null) {
        updateStatus("Load or poll an $adName first")
    } else {
        updateStatus("Showing with a placement override")
        showAd(ad)
    }
}
