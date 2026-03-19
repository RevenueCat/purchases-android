package com.revenuecat.sample.admob.ui.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.loadAndTrackRewardedInterstitialAd
import com.revenuecat.sample.admob.data.Constants

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun RewardedInterstitialAdContent(activity: Activity) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Not Loaded") }
    var rewardedInterstitialAd by remember { mutableStateOf<RewardedInterstitialAd?>(null) }

    Text(
        text = "Interstitial ad that rewards users. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
        style = MaterialTheme.typography.bodySmall,
    )

    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                status = "Loading..."
                Purchases.sharedInstance.adTracker.loadAndTrackRewardedInterstitialAd(
                    context = context,
                    adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                    adRequest = AdRequest.Builder().build(),
                    placement = "home_rewarded_interstitial",
                    loadCallback = object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            rewardedInterstitialAd = ad
                            status = "Loaded - Ready to Show"
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedInterstitialAd = null
                            status = "Failed: ${error.message}"
                        }
                    },
                )
            },
            modifier = Modifier.weight(1f),
            enabled = !status.contains("Loading"),
        ) {
            Text("Load")
        }

        Button(
            onClick = {
                rewardedInterstitialAd?.show(activity) { reward ->
                    Toast.makeText(context, "Earned reward: ${reward.amount} ${reward.type}", Toast.LENGTH_SHORT).show()
                }
                rewardedInterstitialAd = null
                status = "Shown - Load Again"
            },
            modifier = Modifier.weight(1f),
            enabled = rewardedInterstitialAd != null,
        ) {
            Text("Show")
        }
    }
}
