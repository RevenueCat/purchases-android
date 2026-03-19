package com.revenuecat.sample.vanilla.ui.ads

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.sample.vanilla.data.Constants

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
                RewardedInterstitialAd.load(
                    context,
                    Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                    AdRequest.Builder().build(),
                    object : RewardedInterstitialAdLoadCallback() {
                        @Suppress("LongMethod") override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            val responseInfo = ad.responseInfo
                            val adTracker = Purchases.sharedInstance.adTracker
                            adTracker.trackAdLoaded(
                                AdLoadedData(
                                    networkName = responseInfo.mediationAdapterClassName,
                                    mediatorName = AdMediatorName.AD_MOB,
                                    adFormat = AdFormat.REWARDED_INTERSTITIAL,
                                    placement = "home_rewarded_interstitial",
                                    adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                                    impressionId = responseInfo.responseId.orEmpty(),
                                ),
                            )
                            ad.setOnPaidEventListener { adValue ->
                                adTracker.trackAdRevenue(
                                    AdRevenueData(
                                        networkName = responseInfo.mediationAdapterClassName,
                                        mediatorName = AdMediatorName.AD_MOB,
                                        adFormat = AdFormat.REWARDED_INTERSTITIAL,
                                        placement = "home_rewarded_interstitial",
                                        adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                                        impressionId = responseInfo.responseId.orEmpty(),
                                        revenueMicros = adValue.valueMicros,
                                        currency = adValue.currencyCode,
                                        precision = adValue.precisionType.toAdRevenuePrecision(),
                                    ),
                                )
                            }
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    adTracker.trackAdDisplayed(
                                        AdDisplayedData(
                                            networkName = responseInfo.mediationAdapterClassName,
                                            mediatorName = AdMediatorName.AD_MOB,
                                            adFormat = AdFormat.REWARDED_INTERSTITIAL,
                                            placement = "home_rewarded_interstitial",
                                            adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                                            impressionId = responseInfo.responseId.orEmpty(),
                                        ),
                                    )
                                }

                                override fun onAdClicked() {
                                    adTracker.trackAdOpened(
                                        AdOpenedData(
                                            networkName = responseInfo.mediationAdapterClassName,
                                            mediatorName = AdMediatorName.AD_MOB,
                                            adFormat = AdFormat.REWARDED_INTERSTITIAL,
                                            placement = "home_rewarded_interstitial",
                                            adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                                            impressionId = responseInfo.responseId.orEmpty(),
                                        ),
                                    )
                                }

                                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                    status = "Not Loaded"
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    rewardedInterstitialAd = null
                                    status = "Shown - Load Again"
                                }
                            }
                            rewardedInterstitialAd = ad
                            status = "Loaded - Ready to Show"
                            Toast.makeText(context, "Rewarded interstitial loaded!", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Purchases.sharedInstance.adTracker.trackAdFailedToLoad(
                                AdFailedToLoadData(
                                    mediatorName = AdMediatorName.AD_MOB,
                                    adFormat = AdFormat.REWARDED_INTERSTITIAL,
                                    placement = "home_rewarded_interstitial",
                                    adUnitId = Constants.AdMob.REWARDED_INTERSTITIAL_AD_UNIT_ID,
                                    mediatorErrorCode = error.code,
                                ),
                            )
                            rewardedInterstitialAd = null
                            status = "Failed: ${error.message}"
                            Toast.makeText(context, "Failed to load", Toast.LENGTH_SHORT).show()
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
