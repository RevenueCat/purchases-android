@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.sample.admob.nextgen.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.loadAndTrackInterstitialAd
import com.revenuecat.purchases.admob.nextgen.startAndTrack
import kotlinx.coroutines.launch

private const val INVALID_AD_UNIT_ID = "invalid-ad-unit-id"
private const val DIAGNOSTICS_PRELOAD_ID = "sample-invalid-interstitial"

@Composable
@Suppress("LongMethod")
internal fun DiagnosticsScreen(onBack: () -> Unit) {
    var directStatus by remember { mutableStateOf("Trigger an expected direct-load failure") }
    val preloadState = rememberPreloaderUiState(
        DIAGNOSTICS_PRELOAD_ID,
        getConfiguration = { InterstitialAdPreloader.getConfiguration(DIAGNOSTICS_PRELOAD_ID) },
        getNumAdsAvailable = { InterstitialAdPreloader.getNumAdsAvailable(DIAGNOSTICS_PRELOAD_ID) },
    )
    val scope = rememberCoroutineScope()

    AdScreen("Diagnostics", onBack) { mode ->
        Text("Uses an invalid ad unit to exercise RevenueCat failed-to-load tracking.")
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Fail direct load" to {
                    scope.launch {
                        directStatus = "Starting expected direct failure..."
                        runCatching {
                            Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
                                AdRequest.Builder(INVALID_AD_UNIT_ID).build(),
                                placement = "diagnostics_direct",
                            )
                        }.onSuccess { result ->
                            directStatus = when (result) {
                                is AdLoadResult.Success -> "Unexpectedly loaded an ad"
                                is AdLoadResult.Failure -> "Expected failure: ${result.error.message}"
                            }
                        }.onFailure { error ->
                            directStatus = "Request rejected: ${error.message}"
                        }
                    }
                },
            )
        } else {
            PreloaderPanel(
                state = preloadState,
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(InterstitialAdPreloader.destroy(DIAGNOSTICS_PRELOAD_ID))
                    } else {
                        runCatching {
                            InterstitialAdPreloader.startAndTrack(
                                DIAGNOSTICS_PRELOAD_ID,
                                PreloadConfiguration(
                                    AdRequest.Builder(INVALID_AD_UNIT_ID).build(),
                                    preloadState.bufferSize,
                                ),
                                placement = "diagnostics_preload",
                                preloadCallback = preloadState.preloadCallback(scope),
                            )
                        }.onSuccess { started ->
                            preloadState.updateAfterStart(started)
                            if (started) preloadState.message = "Waiting for expected preload failure..."
                        }.onFailure { error ->
                            preloadState.message = "Preload rejected: ${error.message}"
                        }
                    }
                },
            )
        }
    }
}
