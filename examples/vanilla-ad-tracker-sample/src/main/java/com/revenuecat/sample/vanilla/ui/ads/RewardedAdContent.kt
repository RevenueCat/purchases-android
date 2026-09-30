@file:Suppress("LongMethod")

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
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.sample.vanilla.data.Constants
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.rewarded.LevelPlayReward
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener
import kotlin.math.roundToLong

private const val REWARDED_PLACEMENT = "home_rewarded"

@Suppress("MultipleEmitters")
@Composable
internal fun RewardedAdContent(activity: Activity) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Not loaded") }
    val rewardedAd = remember {
        LevelPlayRewardedAd(Constants.LevelPlay.REWARDED_AD_UNIT_ID).apply {
            setListener(object : LevelPlayRewardedAdListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                    Purchases.sharedInstance.adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                            mediatorName = AdMediatorName.LEVEL_PLAY,
                            adFormat = AdFormat.REWARDED,
                            placement = REWARDED_PLACEMENT,
                            adUnitId = Constants.LevelPlay.REWARDED_AD_UNIT_ID,
                            impressionId = adInfo.auctionId.orEmpty(),
                        ),
                    )
                    status = "Loaded - ready to show"
                }

                override fun onAdLoadFailed(error: LevelPlayAdError) {
                    Purchases.sharedInstance.adTracker.trackAdFailedToLoad(
                        AdFailedToLoadData(
                            mediatorName = AdMediatorName.LEVEL_PLAY,
                            adFormat = AdFormat.REWARDED,
                            placement = REWARDED_PLACEMENT,
                            adUnitId = Constants.LevelPlay.REWARDED_AD_UNIT_ID,
                            mediatorErrorCode = error.errorCode,
                        ),
                    )
                    status = "Failed: ${error.errorMessage}"
                }

                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {
                    val adTracker = Purchases.sharedInstance.adTracker
                    adTracker.trackAdDisplayed(
                        AdDisplayedData(
                            networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                            mediatorName = AdMediatorName.LEVEL_PLAY,
                            adFormat = AdFormat.REWARDED,
                            placement = REWARDED_PLACEMENT,
                            adUnitId = Constants.LevelPlay.REWARDED_AD_UNIT_ID,
                            impressionId = adInfo.auctionId.orEmpty(),
                        ),
                    )
                    adTracker.trackAdRevenue(
                        AdRevenueData(
                            networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                            mediatorName = AdMediatorName.LEVEL_PLAY,
                            adFormat = AdFormat.REWARDED,
                            placement = REWARDED_PLACEMENT,
                            adUnitId = Constants.LevelPlay.REWARDED_AD_UNIT_ID,
                            impressionId = adInfo.auctionId.orEmpty(),
                            revenueMicros = (adInfo.revenue * 1_000_000).roundToLong(),
                            currency = "USD",
                            precision = adInfo.precision.toAdRevenuePrecision(),
                        ),
                    )
                    status = "Displayed"
                }

                override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) {
                    status = "Display failed: ${error.errorMessage}"
                }

                override fun onAdClicked(adInfo: LevelPlayAdInfo) {
                    Purchases.sharedInstance.adTracker.trackAdOpened(
                        AdOpenedData(
                            networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                            mediatorName = AdMediatorName.LEVEL_PLAY,
                            adFormat = AdFormat.REWARDED,
                            placement = REWARDED_PLACEMENT,
                            adUnitId = Constants.LevelPlay.REWARDED_AD_UNIT_ID,
                            impressionId = adInfo.auctionId.orEmpty(),
                        ),
                    )
                }

                override fun onAdRewarded(reward: LevelPlayReward, adInfo: LevelPlayAdInfo) {
                    Toast.makeText(
                        context,
                        "Earned reward: ${reward.amount} ${reward.name}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                override fun onAdClosed(adInfo: LevelPlayAdInfo) {
                    status = "Closed - load again"
                }

                override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) = Unit
            })
        }
    }

    Text(
        text = "Full-screen ad that rewards users. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
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
                status = "Loading…"
                rewardedAd.loadAd()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("Load")
        }
        Button(
            onClick = { rewardedAd.showAd(activity) },
            modifier = Modifier.weight(1f),
            enabled = rewardedAd.isAdReady,
        ) {
            Text("Show")
        }
    }
}
