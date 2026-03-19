package com.revenuecat.sample.vanilla.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.sample.vanilla.data.Constants
import kotlinx.coroutines.delay

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ErrorTestingContent() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("") }

    if (status.isNotEmpty()) {
        LaunchedEffect(status) {
            delay(5_000)
            status = ""
        }
    }

    Text(
        text = "Uses invalid ad unit ID to trigger and track ad load failure.",
        style = MaterialTheme.typography.bodySmall,
    )

    if (status.isNotEmpty()) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(
        onClick = {
            status = "Loading with invalid ID..."
            InterstitialAd.load(
                context,
                Constants.AdMob.INVALID_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        // Not expected with an invalid ad unit ID
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Purchases.sharedInstance.adTracker.trackAdFailedToLoad(
                            AdFailedToLoadData(
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.INTERSTITIAL,
                                placement = "error_test",
                                adUnitId = Constants.AdMob.INVALID_AD_UNIT_ID,
                                mediatorErrorCode = error.code,
                            ),
                        )
                        status = "Failed (tracked): ${error.message}"
                    }
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Trigger Ad Load Error")
    }
}
