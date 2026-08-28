package com.revenuecat.sample.admob.nextgen.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.loadAndTrackNativeAd
import com.revenuecat.purchases.admob.nextgen.loadAndTrackNativeAds
import com.revenuecat.purchases.admob.nextgen.pollAndTrackAd
import com.revenuecat.purchases.admob.nextgen.startAndTrack
import com.revenuecat.sample.admob.nextgen.BuildConfig
import com.revenuecat.sample.admob.nextgen.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val NATIVE_PRELOAD_ID = "sample-native"
private const val MIN_NATIVE_BATCH_SIZE = 1
private const val MAX_NATIVE_BATCH_SIZE = 3

@Composable
@Suppress("LongMethod")
internal fun NativeScreen(onBack: () -> Unit) {
    val existingPreloadConfiguration = remember {
        NativeAdPreloader.getConfiguration(NATIVE_PRELOAD_ID)
    }
    var adVariant by remember {
        mutableStateOf(
            if (existingPreloadConfiguration?.request?.adUnitId == NativeAdVariant.VIDEO.adUnitId) {
                NativeAdVariant.VIDEO
            } else {
                NativeAdVariant.STANDARD
            },
        )
    }
    var directStatus by remember { mutableStateOf("No direct native ad loaded") }
    var batchLoading by remember { mutableStateOf(false) }
    var batchSize by remember { mutableIntStateOf(MAX_NATIVE_BATCH_SIZE) }
    var directAds by remember { mutableStateOf(emptyList<NativeAd>()) }
    var preloadedAds by remember { mutableStateOf(emptyList<NativeAd>()) }
    var preloadedAdCount by remember { mutableIntStateOf(0) }
    var loadJob by remember { mutableStateOf<Job?>(null) }
    val preloadState = rememberPreloaderUiState(
        NATIVE_PRELOAD_ID,
        getConfiguration = { NativeAdPreloader.getConfiguration(NATIVE_PRELOAD_ID) },
        getNumAdsAvailable = { NativeAdPreloader.getNumAdsAvailable(NATIVE_PRELOAD_ID) },
    )
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            loadJob?.cancel()
            directAds.destroyAll()
            preloadedAds.destroyAll()
        }
    }

    LaunchedEffect(preloadState.adsAvailable) {
        preloadedAdCount = preloadState.adsAvailable
    }

    AdScreen(
        title = "Native",
        onBack = onBack,
        onModeChange = {
            loadJob?.cancel()
            directAds.destroyAll()
            directAds = emptyList()
            preloadedAds.destroyAll()
            preloadedAds = emptyList()
        },
    ) { mode ->
        Text("Native is the only format with a multi-ad Flow.")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = adVariant == NativeAdVariant.VIDEO,
                onCheckedChange = { useVideoAdUnit ->
                    loadJob?.cancel()
                    directAds.destroyAll()
                    directAds = emptyList()
                    preloadedAds.destroyAll()
                    preloadedAds = emptyList()
                    directStatus = "No direct native ad loaded"
                    adVariant = if (useVideoAdUnit) NativeAdVariant.VIDEO else NativeAdVariant.STANDARD
                },
                enabled = !preloadState.started,
            )
            Text("Native video ad unit")
        }
        if (mode == LoadMode.DIRECT) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(checked = batchLoading, onCheckedChange = { batchLoading = it })
                Text("Batch Flow")
            }
            if (batchLoading) {
                NativeAdCountSetting(
                    batchSize = batchSize,
                    maximum = MAX_NATIVE_BATCH_SIZE,
                    onDecrease = { batchSize = (batchSize - 1).coerceAtLeast(MIN_NATIVE_BATCH_SIZE) },
                    onIncrease = { batchSize = (batchSize + 1).coerceAtMost(MAX_NATIVE_BATCH_SIZE) },
                )
            }
        }
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load + Show" to {
                    loadJob?.cancel()
                    directAds.destroyAll()
                    directAds = emptyList()
                    loadJob = scope.launch {
                        directStatus = if (batchLoading) "Collecting batch Flow..." else "Loading native ad..."
                        if (batchLoading) {
                            Purchases.sharedInstance.adTracker.loadAndTrackNativeAds(
                                nativeRequest(adVariant),
                                maxNumberOfAds = batchSize,
                                placement = adVariant.placement("batch"),
                            ).collect { result ->
                                val handled = handleNativeResult(result, directAds)
                                directAds = handled.ads
                                directStatus = handled.status
                            }
                        } else {
                            val result = Purchases.sharedInstance.adTracker.loadAndTrackNativeAd(
                                nativeRequest(adVariant),
                                placement = adVariant.placement("single"),
                            )
                            val handled = handleNativeResult(result, directAds)
                            directAds = handled.ads
                            directStatus = handled.status
                        }
                    }
                },
            )
        } else {
            PreloaderPanel(
                state = preloadState,
                additionalMetrics = {
                    NativeAdCountSetting(
                        batchSize = preloadedAdCount,
                        maximum = preloadState.adsAvailable,
                        onDecrease = {
                            preloadedAdCount = (preloadedAdCount - 1).coerceAtLeast(MIN_NATIVE_BATCH_SIZE)
                        },
                        onIncrease = {
                            preloadedAdCount = (preloadedAdCount + 1).coerceAtMost(preloadState.adsAvailable)
                        },
                    )
                },
                actions = listOf(
                    PreloaderAction(
                        label = "Poll + Show",
                        enabled = preloadState.started && preloadState.adsAvailable > 0,
                        onClick = {
                            val numberOfAds = preloadedAdCount.coerceAtMost(preloadState.adsAvailable)
                            preloadedAds.destroyAll()
                            preloadedAds = emptyList()
                            val handled = pollNativeAds(numberOfAds, adVariant)
                            preloadState.refresh()
                            if (handled == null) {
                                preloadState.message = "No buffered native result available"
                            } else {
                                preloadedAds = handled.ads
                                preloadState.message = handled.status
                            }
                        },
                    ),
                ),
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(NativeAdPreloader.destroy(NATIVE_PRELOAD_ID))
                    } else {
                        preloadState.updateAfterStart(
                            NativeAdPreloader.startAndTrack(
                                NATIVE_PRELOAD_ID,
                                PreloadConfiguration(nativeRequest(adVariant), preloadState.bufferSize),
                                placement = adVariant.placement("preload"),
                                preloadCallback = preloadState.preloadCallback(scope),
                            ),
                        )
                    }
                },
            )
        }

        val displayedAds = if (mode == LoadMode.DIRECT) directAds else preloadedAds
        displayedAds.forEach { nativeAd ->
            key(nativeAd) { NativeAdCard(nativeAd) }
        }
    }
}

@Composable
private fun NativeAdCountSetting(
    batchSize: Int,
    maximum: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Number of ads", modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onDecrease,
            enabled = batchSize > MIN_NATIVE_BATCH_SIZE,
        ) {
            Text("−")
        }
        Text(batchSize.toString())
        OutlinedButton(
            onClick = onIncrease,
            enabled = batchSize < maximum,
        ) {
            Text("+")
        }
    }
}

private data class HandledNativeResult(val ads: List<NativeAd>, val status: String)

private fun List<NativeAd>.destroyAll() = forEach { it.destroy() }

private fun pollNativeAds(numberOfAds: Int, adVariant: NativeAdVariant): HandledNativeResult? {
    var handledResult: HandledNativeResult? = null
    repeat(numberOfAds) {
        val result = NativeAdPreloader.pollAndTrackAd(
            NATIVE_PRELOAD_ID,
            placement = adVariant.placement("poll"),
        )
        if (result != null) {
            handledResult = handleNativeResult(
                result = result,
                currentAds = handledResult?.ads.orEmpty(),
            )
        }
    }
    return handledResult
}

private fun handleNativeResult(result: NativeAdLoadResult, currentAds: List<NativeAd>): HandledNativeResult {
    return when (result) {
        is NativeAdLoadResult.NativeAdSuccess -> HandledNativeResult(
            ads = currentAds + result.ad,
            status = "Loaded ${currentAds.size + 1} native ad(s)",
        )
        is NativeAdLoadResult.CustomNativeAdSuccess -> HandledNativeResult(
            currentAds,
            "Google returned a custom-native result; tracked but not rendered",
        )
        is NativeAdLoadResult.BannerAdSuccess -> HandledNativeResult(
            currentAds,
            "Google returned banner inventory; tracked but not rendered here",
        )
        is NativeAdLoadResult.Failure -> HandledNativeResult(
            currentAds,
            "Native load failed: ${result.error.message}",
        )
        else -> HandledNativeResult(currentAds, "Unknown native result")
    }
}

@Composable
private fun NativeAdCard(nativeAd: NativeAd) {
    AndroidView(
        factory = { context ->
            val adView = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_layout, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            adView
        },
        modifier = Modifier.fillMaxWidth(),
        onRelease = NativeAdView::destroy,
    )
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    val headline = adView.findViewById<TextView>(R.id.ad_headline)
    val body = adView.findViewById<TextView>(R.id.ad_body)
    val advertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
    val callToAction = adView.findViewById<Button>(R.id.ad_call_to_action)
    val icon = adView.findViewById<ImageView>(R.id.ad_icon)
    val media = adView.findViewById<MediaView>(R.id.ad_media)

    headline.text = nativeAd.headline
    body.text = nativeAd.body
    advertiser.text = nativeAd.advertiser
    callToAction.text = nativeAd.callToAction
    icon.setImageDrawable(nativeAd.icon?.drawable)
    icon.visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
    nativeAd.mediaContent?.let { media.mediaContent = it }

    adView.headlineView = headline
    adView.bodyView = body
    adView.advertiserView = advertiser
    adView.callToActionView = callToAction
    adView.iconView = icon
    adView.registerNativeAd(nativeAd, media)
}

private enum class NativeAdVariant(
    val adUnitId: String,
    private val placementPrefix: String,
) {
    STANDARD(BuildConfig.ADMOB_NATIVE_AD_UNIT_ID, "native"),
    VIDEO(BuildConfig.ADMOB_NATIVE_VIDEO_AD_UNIT_ID, "native_video"),
    ;

    fun placement(stage: String): String = "${placementPrefix}_$stage"
}

private fun nativeRequest(adVariant: NativeAdVariant): NativeAdRequest = NativeAdRequest.Builder(
    adVariant.adUnitId,
    listOf(NativeAd.NativeAdType.NATIVE),
).build()
