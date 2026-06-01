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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.enableRewardVerification
import com.revenuecat.purchases.admob.loadAndTrackRewardedAd
import com.revenuecat.purchases.admob.show
import com.revenuecat.sample.admob.data.Constants
import com.revenuecat.sample.admob.ui.ads.verification.ResultCard
import com.revenuecat.sample.admob.ui.ads.verification.VerificationMessage

@Suppress("MultipleEmitters", "LongMethod")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun RewardedAdContent(activity: Activity) {
    val context = LocalContext.current
    var useRewardVerification by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<VerificationMessage?>(null) }
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    // Captured at load time: the toggle can change before Show, but the show path must match how
    // the ad was loaded (enableRewardVerification() is a precondition of the verification overload).
    var loadedWithVerification by remember { mutableStateOf(false) }

    val isLoading = message?.isLoading == true

    Text(
        text = "Full-screen ad that rewards users. Tracks: Loaded, Displayed, Opened (on click), Revenue. " +
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
                message = VerificationMessage.loading
                val verifyReward = useRewardVerification
                Purchases.sharedInstance.adTracker.loadAndTrackRewardedAd(
                    context = context,
                    adUnitId = Constants.AdMob.REWARDED_AD_UNIT_ID,
                    adRequest = AdRequest.Builder().build(),
                    placement = "home_rewarded",
                    loadCallback = object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            if (verifyReward) {
                                ad.enableRewardVerification()
                                message = VerificationMessage.readyWithVerification
                            } else {
                                message = VerificationMessage.readyWithoutVerification
                            }
                            loadedWithVerification = verifyReward
                            rewardedAd = ad
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedAd = null
                            message = VerificationMessage.loadFailed
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
                val ad = rewardedAd ?: return@Button
                if (loadedWithVerification) {
                    ad.show(
                        activity,
                        rewardVerificationStarted = { message = VerificationMessage.verifyingReward },
                        rewardVerificationCompleted = { result ->
                            message = VerificationMessage.forVerificationResult(result)
                        },
                    )
                } else {
                    ad.show(
                        activity,
                        OnUserEarnedRewardListener { reward ->
                            message = VerificationMessage.rewardEarned(reward.amount, reward.type)
                            Toast.makeText(
                                context,
                                "Earned reward: ${reward.amount} ${reward.type}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
                rewardedAd = null
            },
            modifier = Modifier.weight(1f),
            enabled = rewardedAd != null,
        ) {
            Text("Show")
        }
    }

    message?.let { ResultCard(it) }
}
