package com.revenuecat.sample.admob.ui.ads

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.enableRewardVerification
import com.revenuecat.purchases.admob.loadAndTrackRewardedInterstitialAd
import com.revenuecat.purchases.admob.show
import com.revenuecat.sample.admob.data.Constants
import com.revenuecat.sample.admob.ui.ads.verification.RewardedResultCard
import com.revenuecat.sample.admob.ui.ads.verification.RewardedVerificationMessage

@Suppress("MultipleEmitters", "LongMethod")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun RewardedInterstitialAdContent(activity: Activity) {
    val context = LocalContext.current
    var useRewardVerification by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<RewardedVerificationMessage?>(null) }
    var rewardedInterstitialAd by remember { mutableStateOf<RewardedInterstitialAd?>(null) }
    // Captured at load time: the toggle can change before Show, but the show path must match how
    // the ad was loaded (enableRewardVerification() is a precondition of the verification overload).
    var loadedWithVerification by remember { mutableStateOf(false) }

    val isLoading = message?.isLoading == true

    Text(
        text = "Interstitial ad that rewards users. Tracks: Loaded, Displayed, Opened (on click), Revenue. " +
            "Toggle reward verification to validate the reward server-side via RevenueCat.",
        style = MaterialTheme.typography.bodySmall,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = useRewardVerification,
            onCheckedChange = { useRewardVerification = it },
            enabled = !isLoading,
        )
        Column {
            Text(text = "Reward Verification", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Applies to the next loaded ad",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                message = RewardedVerificationMessage.loading
                val verifyReward = useRewardVerification
                Purchases.sharedInstance.adTracker.loadAndTrackRewardedInterstitialAd(
                    context = context,
                    adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                    adRequest = AdRequest.Builder().build(),
                    placement = "home_rewarded_interstitial",
                    loadCallback = object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            if (verifyReward) {
                                ad.enableRewardVerification()
                                message = RewardedVerificationMessage.readyWithVerification
                            } else {
                                message = RewardedVerificationMessage.readyWithoutVerification
                            }
                            loadedWithVerification = verifyReward
                            rewardedInterstitialAd = ad
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedInterstitialAd = null
                            message = RewardedVerificationMessage.loadFailed
                        }
                    },
                )
            },
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
        ) {
            Text("Load")
        }

        Button(
            onClick = {
                val ad = rewardedInterstitialAd ?: return@Button
                if (loadedWithVerification) {
                    ad.show(
                        activity,
                        rewardVerificationStarted = { message = RewardedVerificationMessage.verifyingReward },
                        rewardVerificationCompleted = { result ->
                            message = RewardedVerificationMessage.forVerificationResult(result)
                        },
                    )
                } else {
                    ad.show(
                        activity,
                        OnUserEarnedRewardListener { reward ->
                            message = RewardedVerificationMessage.rewardEarned(reward.amount, reward.type)
                            Toast.makeText(
                                context,
                                "Earned reward: ${reward.amount} ${reward.type}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
                rewardedInterstitialAd = null
            },
            modifier = Modifier.weight(1f),
            enabled = rewardedInterstitialAd != null,
        ) {
            Text("Show")
        }
    }

    message?.let { RewardedResultCard(it) }
}
