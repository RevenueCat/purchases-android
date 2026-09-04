package com.revenuecat.sample.admob.nextgen.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.enableRewardVerification
import com.revenuecat.purchases.admob.nextgen.loadAndTrackRewardedAd
import com.revenuecat.purchases.admob.nextgen.loadAndTrackRewardedInterstitialAd
import com.revenuecat.purchases.admob.nextgen.pollAndTrackAd
import com.revenuecat.purchases.admob.nextgen.show
import com.revenuecat.purchases.admob.nextgen.startAndTrack
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.sample.admob.nextgen.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val REWARDED_PRELOAD_ID = "sample-rewarded"
private const val REWARDED_INTERSTITIAL_PRELOAD_ID = "sample-rewarded-interstitial"

@Composable
internal fun RewardedScreen(activity: Activity, onBack: () -> Unit) = RewardedAdScreen(
    title = "Rewarded",
    description = "The verification choice is captured when an ad is loaded or polled.",
    adName = "rewarded ad",
    preloadId = REWARDED_PRELOAD_ID,
    onBack = onBack,
    getConfiguration = { RewardedAdPreloader.getConfiguration(REWARDED_PRELOAD_ID) },
    getNumAdsAvailable = { RewardedAdPreloader.getNumAdsAvailable(REWARDED_PRELOAD_ID) },
    loadAd = {
        Purchases.sharedInstance.adTracker.loadAndTrackRewardedAd(
            AdRequest.Builder(BuildConfig.ADMOB_REWARDED_AD_UNIT_ID).build(),
            placement = "rewarded_load",
        )
    },
    pollAd = {
        RewardedAdPreloader.pollAndTrackAd(
            REWARDED_PRELOAD_ID,
            placement = "rewarded_poll",
        )
    },
    startPreloader = { bufferSize, callback ->
        RewardedAdPreloader.startAndTrack(
            REWARDED_PRELOAD_ID,
            PreloadConfiguration(
                AdRequest.Builder(BuildConfig.ADMOB_REWARDED_AD_UNIT_ID).build(),
                bufferSize,
            ),
            placement = "rewarded_preload",
            preloadCallback = callback,
        )
    },
    stopPreloader = { RewardedAdPreloader.destroy(REWARDED_PRELOAD_ID) },
    showActions = RewardedShowActions<RewardedAd>(
        enableVerification = { ad -> ad.enableRewardVerification() },
        showVerifiedAd = { ad, onVerificationStarted, onVerificationCompleted ->
            ad.show(
                activity,
                placement = "rewarded_show",
                rewardVerificationStarted = onVerificationStarted,
                rewardVerificationCompleted = onVerificationCompleted,
            )
        },
        showUnverifiedAd = { ad, listener ->
            ad.show(
                activity,
                placement = "rewarded_show",
                onUserEarnedRewardListener = listener,
            )
        },
    ),
)

@Composable
internal fun RewardedInterstitialScreen(activity: Activity, onBack: () -> Unit) = RewardedAdScreen(
    title = "Rewarded interstitial",
    description = "Both direct and preloaded ads can opt into RevenueCat reward verification.",
    adName = "rewarded-interstitial ad",
    preloadId = REWARDED_INTERSTITIAL_PRELOAD_ID,
    onBack = onBack,
    getConfiguration = {
        RewardedInterstitialAdPreloader.getConfiguration(REWARDED_INTERSTITIAL_PRELOAD_ID)
    },
    getNumAdsAvailable = {
        RewardedInterstitialAdPreloader.getNumAdsAvailable(REWARDED_INTERSTITIAL_PRELOAD_ID)
    },
    loadAd = {
        Purchases.sharedInstance.adTracker.loadAndTrackRewardedInterstitialAd(
            AdRequest.Builder(BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID).build(),
            placement = "rewarded_interstitial_load",
        )
    },
    pollAd = {
        RewardedInterstitialAdPreloader.pollAndTrackAd(
            REWARDED_INTERSTITIAL_PRELOAD_ID,
            placement = "rewarded_interstitial_poll",
        )
    },
    startPreloader = { bufferSize, callback ->
        RewardedInterstitialAdPreloader.startAndTrack(
            REWARDED_INTERSTITIAL_PRELOAD_ID,
            PreloadConfiguration(
                AdRequest.Builder(BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID).build(),
                bufferSize,
            ),
            placement = "rewarded_interstitial_preload",
            preloadCallback = callback,
        )
    },
    stopPreloader = { RewardedInterstitialAdPreloader.destroy(REWARDED_INTERSTITIAL_PRELOAD_ID) },
    showActions = RewardedShowActions<RewardedInterstitialAd>(
        enableVerification = { ad -> ad.enableRewardVerification() },
        showVerifiedAd = { ad, onVerificationStarted, onVerificationCompleted ->
            ad.show(
                activity,
                placement = "rewarded_interstitial_show",
                rewardVerificationStarted = onVerificationStarted,
                rewardVerificationCompleted = onVerificationCompleted,
            )
        },
        showUnverifiedAd = { ad, listener ->
            ad.show(
                activity,
                placement = "rewarded_interstitial_show",
                onUserEarnedRewardListener = listener,
            )
        },
    ),
)

private data class RewardedShowActions<AdT : Ad>(
    val enableVerification: (AdT) -> Unit,
    val showVerifiedAd: (AdT, () -> Unit, (RewardVerificationResult) -> Unit) -> Unit,
    val showUnverifiedAd: (AdT, OnUserEarnedRewardListener) -> Unit,
)

@Composable
@Suppress("LongMethod", "LongParameterList")
private fun <AdT : Ad> RewardedAdScreen(
    title: String,
    description: String,
    adName: String,
    preloadId: String,
    onBack: () -> Unit,
    getConfiguration: () -> PreloadConfiguration?,
    getNumAdsAvailable: () -> Int,
    loadAd: suspend () -> AdLoadResult<AdT>,
    pollAd: () -> AdT?,
    startPreloader: (Int, PreloadCallback) -> Boolean,
    stopPreloader: () -> Boolean,
    showActions: RewardedShowActions<AdT>,
) {
    var directStatus by remember { mutableStateOf("No direct $adName loaded") }
    var useVerification by remember { mutableStateOf(false) }
    var directLoadedWithVerification by remember { mutableStateOf(false) }
    var directAd by remember { mutableStateOf<AdT?>(null) }
    val preloadState = rememberPreloaderUiState(
        preloadId,
        getConfiguration = getConfiguration,
        getNumAdsAvailable = getNumAdsAvailable,
    )
    val scope = rememberCoroutineScope()

    AdScreen(title, onBack) { mode ->
        RewardVerificationToggle(useVerification) { useVerification = it }
        Text(description)
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load" to {
                    scope.launch {
                        val verify = useVerification
                        directStatus = "Loading directly..."
                        when (val result = loadAd()) {
                            is AdLoadResult.Success -> {
                                directAd = result.ad.also { if (verify) showActions.enableVerification(it) }
                                directLoadedWithVerification = verify
                                directStatus = readyStatus(verify)
                            }
                            is AdLoadResult.Failure -> directStatus = "Load failed: ${result.error.message}"
                        }
                    }
                },
                "Show" to {
                    val loadedAd = directAd
                    if (loadedAd == null) {
                        directStatus = "Load a $adName first"
                    } else {
                        showRewardedAd(
                            ad = loadedAd,
                            verify = directLoadedWithVerification,
                            scope = scope,
                            showActions = showActions,
                            updateStatus = { directStatus = it },
                        )
                        directAd = null
                    }
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
                            val verify = useVerification
                            val ad = pollAd()?.also { if (verify) showActions.enableVerification(it) }
                            preloadState.refresh()
                            if (ad == null) {
                                preloadState.message = "No buffered $adName available"
                            } else {
                                preloadState.message = "Showing $adName"
                                showRewardedAd(
                                    ad = ad,
                                    verify = verify,
                                    scope = scope,
                                    showActions = showActions,
                                    updateStatus = { preloadState.message = it },
                                )
                            }
                        },
                    ),
                ),
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(stopPreloader())
                    } else {
                        preloadState.updateAfterStart(
                            startPreloader(preloadState.bufferSize, preloadState.preloadCallback(scope)),
                        )
                    }
                },
            )
        }
    }
}

private fun <AdT : Ad> showRewardedAd(
    ad: AdT,
    verify: Boolean,
    scope: CoroutineScope,
    showActions: RewardedShowActions<AdT>,
    updateStatus: (String) -> Unit,
) {
    if (verify) {
        showActions.showVerifiedAd(
            ad,
            { updateStatus("Verifying reward...") },
            { result -> updateStatus("Verification complete: ${result.verifiedReward ?: "failed"}") },
        )
    } else {
        showActions.showUnverifiedAd(
            ad,
            OnUserEarnedRewardListener { reward ->
                scope.launch {
                    updateStatus("Reward earned: ${reward.amount} ${reward.type}")
                }
            },
        )
    }
}

@Composable
private fun RewardVerificationToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text("RevenueCat reward verification")
    }
}

private fun readyStatus(verificationEnabled: Boolean): String = if (verificationEnabled) {
    "Ad ready with reward verification enabled"
} else {
    "Ad ready with Google's reward callback"
}
