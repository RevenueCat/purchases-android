@file:Suppress("LongMethod")

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
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
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
import com.revenuecat.sample.admob.nextgen.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val REWARDED_PRELOAD_ID = "sample-rewarded"
private const val REWARDED_INTERSTITIAL_PRELOAD_ID = "sample-rewarded-interstitial"

@Composable
internal fun RewardedScreen(activity: Activity, onBack: () -> Unit) {
    var directStatus by remember { mutableStateOf("No direct rewarded ad loaded") }
    var useVerification by remember { mutableStateOf(false) }
    var directLoadedWithVerification by remember { mutableStateOf(false) }
    var directAd by remember { mutableStateOf<RewardedAd?>(null) }
    val preloadState = rememberPreloaderUiState(
        REWARDED_PRELOAD_ID,
        getConfiguration = { RewardedAdPreloader.getConfiguration(REWARDED_PRELOAD_ID) },
        getNumAdsAvailable = { RewardedAdPreloader.getNumAdsAvailable(REWARDED_PRELOAD_ID) },
    )
    val scope = rememberCoroutineScope()

    AdScreen("Rewarded", onBack) { mode ->
        RewardVerificationToggle(useVerification) { useVerification = it }
        Text("The verification choice is captured when an ad is loaded or polled.")
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load" to {
                    scope.launch {
                        val verify = useVerification
                        directStatus = "Loading directly..."
                        when (
                            val result = Purchases.sharedInstance.adTracker.loadAndTrackRewardedAd(
                                AdRequest.Builder(BuildConfig.ADMOB_REWARDED_AD_UNIT_ID).build(),
                                placement = "rewarded_load",
                            )
                        ) {
                            is AdLoadResult.Success -> {
                                directAd = result.ad.also { if (verify) it.enableRewardVerification() }
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
                        directStatus = "Load a rewarded ad first"
                    } else {
                        showRewarded(loadedAd, activity, directLoadedWithVerification, scope) {
                            directStatus = it
                        }
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
                            val ad = RewardedAdPreloader.pollAndTrackAd(
                                REWARDED_PRELOAD_ID,
                                placement = "rewarded_poll",
                            )?.also { if (verify) it.enableRewardVerification() }
                            preloadState.refresh()
                            if (ad == null) {
                                preloadState.message = "No buffered rewarded ad available"
                            } else {
                                preloadState.message = "Showing rewarded ad"
                                showRewarded(ad, activity, verify, scope) { preloadState.message = it }
                            }
                        },
                    ),
                ),
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(RewardedAdPreloader.destroy(REWARDED_PRELOAD_ID))
                    } else {
                        preloadState.updateAfterStart(
                            RewardedAdPreloader.startAndTrack(
                                REWARDED_PRELOAD_ID,
                                PreloadConfiguration(
                                    AdRequest.Builder(BuildConfig.ADMOB_REWARDED_AD_UNIT_ID).build(),
                                    preloadState.bufferSize,
                                ),
                                placement = "rewarded_preload",
                                preloadCallback = preloadState.preloadCallback(scope),
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun showRewarded(
    ad: RewardedAd,
    activity: Activity,
    verify: Boolean,
    scope: CoroutineScope,
    updateStatus: (String) -> Unit,
) {
    if (verify) {
        ad.show(
            activity,
            placement = "rewarded_show",
            rewardVerificationStarted = { updateStatus("Verifying reward...") },
            rewardVerificationCompleted = { result ->
                updateStatus("Verification complete: ${result.verifiedReward ?: "failed"}")
            },
        )
    } else {
        ad.show(
            activity,
            placement = "rewarded_show",
            onUserEarnedRewardListener = OnUserEarnedRewardListener { reward ->
                scope.launch {
                    updateStatus("Reward earned: ${reward.amount} ${reward.type}")
                }
            },
        )
    }
}

@Composable
internal fun RewardedInterstitialScreen(activity: Activity, onBack: () -> Unit) {
    var directStatus by remember { mutableStateOf("No direct rewarded-interstitial ad loaded") }
    var useVerification by remember { mutableStateOf(false) }
    var directLoadedWithVerification by remember { mutableStateOf(false) }
    var directAd by remember { mutableStateOf<RewardedInterstitialAd?>(null) }
    val preloadState = rememberPreloaderUiState(
        REWARDED_INTERSTITIAL_PRELOAD_ID,
        getConfiguration = {
            RewardedInterstitialAdPreloader.getConfiguration(REWARDED_INTERSTITIAL_PRELOAD_ID)
        },
        getNumAdsAvailable = {
            RewardedInterstitialAdPreloader.getNumAdsAvailable(REWARDED_INTERSTITIAL_PRELOAD_ID)
        },
    )
    val scope = rememberCoroutineScope()

    AdScreen("Rewarded interstitial", onBack) { mode ->
        RewardVerificationToggle(useVerification) { useVerification = it }
        Text("Both direct and preloaded ads can opt into RevenueCat reward verification.")
        if (mode == LoadMode.DIRECT) {
            StatusCard(directStatus)
            ActionRow(
                "Load" to {
                    scope.launch {
                        val verify = useVerification
                        directStatus = "Loading directly..."
                        when (
                            val result = Purchases.sharedInstance.adTracker.loadAndTrackRewardedInterstitialAd(
                                AdRequest.Builder(BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID).build(),
                                placement = "rewarded_interstitial_load",
                            )
                        ) {
                            is AdLoadResult.Success -> {
                                directAd = result.ad.also { if (verify) it.enableRewardVerification() }
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
                        directStatus = "Load a rewarded-interstitial ad first"
                    } else {
                        showRewardedInterstitial(loadedAd, activity, directLoadedWithVerification, scope) {
                            directStatus = it
                        }
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
                            val ad = RewardedInterstitialAdPreloader.pollAndTrackAd(
                                REWARDED_INTERSTITIAL_PRELOAD_ID,
                                placement = "rewarded_interstitial_poll",
                            )?.also { if (verify) it.enableRewardVerification() }
                            preloadState.refresh()
                            if (ad == null) {
                                preloadState.message = "No buffered rewarded-interstitial ad available"
                            } else {
                                preloadState.message = "Showing rewarded-interstitial ad"
                                showRewardedInterstitial(ad, activity, verify, scope) {
                                    preloadState.message = it
                                }
                            }
                        },
                    ),
                ),
                onToggle = {
                    if (preloadState.started) {
                        preloadState.updateAfterStop(
                            RewardedInterstitialAdPreloader.destroy(REWARDED_INTERSTITIAL_PRELOAD_ID),
                        )
                    } else {
                        preloadState.updateAfterStart(
                            RewardedInterstitialAdPreloader.startAndTrack(
                                REWARDED_INTERSTITIAL_PRELOAD_ID,
                                PreloadConfiguration(
                                    AdRequest.Builder(BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID).build(),
                                    preloadState.bufferSize,
                                ),
                                placement = "rewarded_interstitial_preload",
                                preloadCallback = preloadState.preloadCallback(scope),
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun showRewardedInterstitial(
    ad: RewardedInterstitialAd,
    activity: Activity,
    verify: Boolean,
    scope: CoroutineScope,
    updateStatus: (String) -> Unit,
) {
    if (verify) {
        ad.show(
            activity,
            placement = "rewarded_interstitial_show",
            rewardVerificationStarted = { updateStatus("Verifying reward...") },
            rewardVerificationCompleted = { result ->
                updateStatus("Verification complete: ${result.verifiedReward ?: "failed"}")
            },
        )
    } else {
        ad.show(
            activity,
            placement = "rewarded_interstitial_show",
            onUserEarnedRewardListener = OnUserEarnedRewardListener { reward ->
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
