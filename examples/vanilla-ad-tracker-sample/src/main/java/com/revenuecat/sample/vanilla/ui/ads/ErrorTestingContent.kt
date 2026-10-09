package com.revenuecat.sample.vanilla.ui.ads

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
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.sample.vanilla.data.Constants
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener

@Suppress("MultipleEmitters")
@Composable
internal fun ErrorTestingContent() {
    var status by remember { mutableStateOf("Not loaded") }
    val errorAd = remember {
        LevelPlayInterstitialAd(Constants.LevelPlay.INVALID_AD_UNIT_ID).apply {
            setListener(object : LevelPlayInterstitialAdListener {
                override fun onAdLoadFailed(error: LevelPlayAdError) {
                    Purchases.sharedInstance.adTracker.trackAdFailedToLoad(
                        AdFailedToLoadData(
                            mediatorName = AdMediatorName.LEVEL_PLAY,
                            adFormat = AdFormat.INTERSTITIAL,
                            placement = "error_test",
                            adUnitId = Constants.LevelPlay.INVALID_AD_UNIT_ID,
                            mediatorErrorCode = error.errorCode,
                        ),
                    )
                    status = "Failed (tracked): ${error.errorMessage}"
                }

                override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                    status = "Unexpectedly loaded"
                }

                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) = Unit
                override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) = Unit
                override fun onAdClicked(adInfo: LevelPlayAdInfo) = Unit
                override fun onAdClosed(adInfo: LevelPlayAdInfo) = Unit
                override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) = Unit
            })
        }
    }

    Text(
        text = "Uses an invalid LevelPlay ad unit ID to trigger and track an ad load failure.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = {
            status = "Loading with invalid ID…"
            errorAd.loadAd()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Trigger Ad Load Error")
    }
}
