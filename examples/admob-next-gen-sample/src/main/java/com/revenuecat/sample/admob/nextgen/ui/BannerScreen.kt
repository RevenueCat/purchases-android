package com.revenuecat.sample.admob.nextgen.ui

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.revenuecat.purchases.admob.nextgen.loadAndTrackAd
import com.revenuecat.purchases.admob.nextgen.pollAndTrackAd
import com.revenuecat.purchases.admob.nextgen.setTrackingAdEventCallback
import com.revenuecat.purchases.admob.nextgen.setTrackingBannerAdRefreshCallback
import com.revenuecat.purchases.admob.nextgen.startAndTrack
import com.revenuecat.sample.admob.nextgen.BuildConfig
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val BANNER_PRELOAD_ID = "sample-banner"

@Composable
@Suppress("LongMethod")
internal fun BannerScreen(activity: Activity, onBack: () -> Unit) {
    var status by remember { mutableStateOf("Banner view is ready") }
    val preloadState = rememberPreloaderUiState(
        BANNER_PRELOAD_ID,
        getConfiguration = { BannerAdPreloader.getConfiguration(BANNER_PRELOAD_ID) },
        getNumAdsAvailable = { BannerAdPreloader.getNumAdsAvailable(BANNER_PRELOAD_ID) },
    )
    val scope = rememberCoroutineScope()

    AdScreen("Banner", onBack) { mode ->
        key(mode) {
            val active = remember { AtomicBoolean(true) }
            val adView = remember(activity, mode) { AdView(activity) }
            val updateStatus: (String) -> Unit = { message ->
                if (mode == LoadMode.DIRECT) status = message else preloadState.message = message
            }
            val eventCallback = remember {
                object : BannerAdEventCallback {
                    override fun onAdImpression() {
                        if (active.get()) scope.launch { updateStatus("Banner impression") }
                    }

                    override fun onAdClicked() {
                        if (active.get()) scope.launch { updateStatus("Banner clicked") }
                    }
                }
            }
            val refreshCallback = remember {
                object : BannerAdRefreshCallback {
                    override fun onAdRefreshed() {
                        if (active.get()) scope.launch { updateStatus("Banner refreshed") }
                    }

                    override fun onAdFailedToRefresh(error: LoadAdError) {
                        if (active.get()) scope.launch { updateStatus("Refresh failed: ${error.message}") }
                    }
                }
            }

            DisposableEffect(adView) {
                onDispose {
                    active.set(false)
                    adView.destroy()
                }
            }
            LaunchedEffect(mode) {
                status = "${mode.label} banner view is ready"
            }

            Text("Preloaded banners are polled first, then registered with the existing AdView.")
            if (mode == LoadMode.DIRECT) {
                StatusCard(status)
                ActionRow(
                    "Load" to {
                        status = "Loading directly..."
                        adView.loadAndTrackAd(
                            adRequest = bannerRequest(),
                            placement = "banner_direct",
                            loadCallback = object : AdLoadCallback<BannerAd> {
                                override fun onAdLoaded(ad: BannerAd) {
                                    ad.setTrackingAdEventCallback(eventCallback)
                                    ad.setTrackingBannerAdRefreshCallback(refreshCallback)
                                    if (active.get()) {
                                        scope.launch { updateStatus("Banner ready") }
                                    }
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    if (active.get()) scope.launch { updateStatus("Load failed: ${error.message}") }
                                }
                            },
                        )
                    },
                )
            } else {
                PreloaderPanel(
                    state = preloadState,
                    actions = listOf(
                        PreloaderAction(
                            label = "Poll + Show",
                            enabled = preloadState.started && preloadState.adsAvailable > 0,
                            onClick = {
                                val ad = BannerAdPreloader.pollAndTrackAd(
                                    BANNER_PRELOAD_ID,
                                    placement = "banner_poll",
                                    adEventCallback = eventCallback,
                                    bannerAdRefreshCallback = refreshCallback,
                                )
                                preloadState.refresh()
                                if (ad == null) {
                                    preloadState.message = "No buffered banner available"
                                } else {
                                    adView.registerBannerAd(ad, activity)
                                    preloadState.message = "Buffered banner registered"
                                }
                            },
                        ),
                    ),
                    onToggle = {
                        if (preloadState.started) {
                            preloadState.updateAfterStop(BannerAdPreloader.destroy(BANNER_PRELOAD_ID))
                        } else {
                            preloadState.updateAfterStart(
                                BannerAdPreloader.startAndTrack(
                                    BANNER_PRELOAD_ID,
                                    PreloadConfiguration(bannerRequest(), preloadState.bufferSize),
                                    placement = "banner_preload",
                                    preloadCallback = preloadState.preloadCallback(scope),
                                ),
                            )
                        }
                    },
                )
            }

            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}

private fun bannerRequest(): BannerAdRequest = BannerAdRequest.Builder(
    BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
    AdSize.BANNER,
).build()
