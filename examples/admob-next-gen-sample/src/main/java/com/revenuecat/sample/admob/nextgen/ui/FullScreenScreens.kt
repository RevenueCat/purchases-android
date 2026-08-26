package com.revenuecat.sample.admob.nextgen.ui

import android.app.Activity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.loadAndTrackAppOpenAd
import com.revenuecat.purchases.admob.nextgen.loadAndTrackInterstitialAd
import com.revenuecat.purchases.admob.nextgen.pollAndTrackAd
import com.revenuecat.purchases.admob.nextgen.setTrackingAdEventCallback
import com.revenuecat.purchases.admob.nextgen.show
import com.revenuecat.purchases.admob.nextgen.startAndTrack
import com.revenuecat.sample.admob.nextgen.BuildConfig
import kotlinx.coroutines.launch

private const val INTERSTITIAL_PRELOAD_ID = "sample-interstitial"
private const val APP_OPEN_PRELOAD_ID = "sample-app-open"

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun InterstitialScreen(activity: Activity, onBack: () -> Unit) {
    var directStatus by remember { mutableStateOf("No direct interstitial loaded") }
    var directAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val preloadState = rememberPreloaderUiState(
        INTERSTITIAL_PRELOAD_ID,
        getConfiguration = { InterstitialAdPreloader.getConfiguration(INTERSTITIAL_PRELOAD_ID) },
        getNumAdsAvailable = { InterstitialAdPreloader.getNumAdsAvailable(INTERSTITIAL_PRELOAD_ID) },
    )
    val scope = rememberCoroutineScope()
    val directEventCallback = remember {
        object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                scope.launch {
                    directAd = null
                    directStatus = "Direct interstitial dismissed"
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                scope.launch {
                    directAd = null
                    directStatus = "Show failed: ${error.message}"
                }
            }
        }
    }
    val preloadedEventCallback = remember {
        object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                scope.launch {
                    preloadState.message = "Preloaded interstitial dismissed"
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                scope.launch {
                    preloadState.message = "Show failed: ${error.message}"
                }
            }
        }
    }

    AdScreen("Interstitial", onBack) { mode ->
        Text("Load placement and show placement are deliberately different to demonstrate show-time overrides.")
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load" to {
                    scope.launch {
                        directStatus = "Loading directly..."
                        when (
                            val result = Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
                                AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID).build(),
                                placement = "interstitial_load",
                            )
                        ) {
                            is AdLoadResult.Success -> {
                                directAd = result.ad.also { it.setTrackingAdEventCallback(directEventCallback) }
                                directStatus = "Direct interstitial ready"
                            }
                            is AdLoadResult.Failure -> directStatus = "Load failed: ${result.error.message}"
                        }
                    }
                },
                "Show" to { showInterstitial(directAd, activity) { directStatus = it } },
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
                            val ad = InterstitialAdPreloader.pollAndTrackAd(
                                INTERSTITIAL_PRELOAD_ID,
                                placement = "interstitial_poll",
                                adEventCallback = preloadedEventCallback,
                            )
                            preloadState.refresh()
                            if (ad == null) {
                                preloadState.message = "No buffered ad available"
                            } else {
                                showInterstitial(ad, activity) { preloadState.message = it }
                            }
                        },
                    ),
                ),
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(
                            InterstitialAdPreloader.destroy(INTERSTITIAL_PRELOAD_ID),
                        )
                    } else {
                        preloadState.updateAfterStart(
                            InterstitialAdPreloader.startAndTrack(
                                preloadId = INTERSTITIAL_PRELOAD_ID,
                                preloadConfiguration = PreloadConfiguration(
                                    AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID).build(),
                                    preloadState.bufferSize,
                                ),
                                placement = "interstitial_preload",
                                preloadCallback = preloadState.preloadCallback(scope),
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun showInterstitial(ad: InterstitialAd?, activity: Activity, updateStatus: (String) -> Unit) {
    if (ad == null) {
        updateStatus("Load or poll an interstitial first")
    } else {
        updateStatus("Showing with a placement override")
        ad.show(activity, placement = "interstitial_show")
    }
}

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun AppOpenScreen(activity: Activity, onBack: () -> Unit) {
    var directStatus by remember { mutableStateOf("No direct app-open ad loaded") }
    var directAd by remember { mutableStateOf<AppOpenAd?>(null) }
    val preloadState = rememberPreloaderUiState(
        APP_OPEN_PRELOAD_ID,
        getConfiguration = { AppOpenAdPreloader.getConfiguration(APP_OPEN_PRELOAD_ID) },
        getNumAdsAvailable = { AppOpenAdPreloader.getNumAdsAvailable(APP_OPEN_PRELOAD_ID) },
    )
    val scope = rememberCoroutineScope()
    val directEventCallback = remember {
        object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                scope.launch {
                    directAd = null
                    directStatus = "Direct app-open ad dismissed"
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                scope.launch {
                    directAd = null
                    directStatus = "Show failed: ${error.message}"
                }
            }
        }
    }
    val preloadedEventCallback = remember {
        object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                scope.launch {
                    preloadState.message = "Preloaded app-open ad dismissed"
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                scope.launch {
                    preloadState.message = "Show failed: ${error.message}"
                }
            }
        }
    }

    AdScreen("App open", onBack) { mode ->
        Text("The sample exposes app-open loading explicitly instead of tying it to process lifecycle.")
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load" to {
                    scope.launch {
                        directStatus = "Loading directly..."
                        when (
                            val result = Purchases.sharedInstance.adTracker.loadAndTrackAppOpenAd(
                                AdRequest.Builder(BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID).build(),
                                placement = "app_open_load",
                                adEventCallback = directEventCallback,
                            )
                        ) {
                            is AdLoadResult.Success -> {
                                directAd = result.ad
                                directStatus = "Direct app-open ad ready"
                            }
                            is AdLoadResult.Failure -> directStatus = "Load failed: ${result.error.message}"
                        }
                    }
                },
                "Show" to { showAppOpen(directAd, activity) { directStatus = it } },
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
                            val ad = AppOpenAdPreloader.pollAndTrackAd(
                                APP_OPEN_PRELOAD_ID,
                                placement = "app_open_poll",
                                adEventCallback = preloadedEventCallback,
                            )
                            preloadState.refresh()
                            if (ad == null) {
                                preloadState.message = "No buffered ad available"
                            } else {
                                showAppOpen(ad, activity) { preloadState.message = it }
                            }
                        },
                    ),
                ),
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(AppOpenAdPreloader.destroy(APP_OPEN_PRELOAD_ID))
                    } else {
                        preloadState.updateAfterStart(
                            AppOpenAdPreloader.startAndTrack(
                                APP_OPEN_PRELOAD_ID,
                                PreloadConfiguration(
                                    AdRequest.Builder(BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID).build(),
                                    preloadState.bufferSize,
                                ),
                                placement = "app_open_preload",
                                preloadCallback = preloadState.preloadCallback(scope),
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun showAppOpen(ad: AppOpenAd?, activity: Activity, updateStatus: (String) -> Unit) {
    if (ad == null) {
        updateStatus("Load or poll an app-open ad first")
    } else {
        updateStatus("Showing with a placement override")
        ad.show(activity, placement = "app_open_show")
    }
}
