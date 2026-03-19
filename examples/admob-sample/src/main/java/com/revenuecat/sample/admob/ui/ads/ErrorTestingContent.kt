package com.revenuecat.sample.admob.ui.ads

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
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.loadAndTrackInterstitialAd
import com.revenuecat.sample.admob.data.Constants
import kotlinx.coroutines.delay

private const val ERROR_FEEDBACK_DURATION_MS = 5_000L

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ErrorTestingContent() {
    val context = LocalContext.current
    var showFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(ERROR_FEEDBACK_DURATION_MS)
            showFeedback = false
        }
    }

    Text(
        text = "Uses invalid ad unit ID to trigger and track ad load failure.",
        style = MaterialTheme.typography.bodySmall,
    )

    Button(
        onClick = {
            Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
                context = context,
                adUnitId = Constants.AdMob.INVALID_AD_UNIT_ID,
                adRequest = AdRequest.Builder().build(),
                placement = "error_test",
            )
            showFeedback = true
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Trigger Ad Load Error")
    }

    if (showFeedback) {
        Text(
            text = "Loading with invalid ID. Check Logcat for failure tracking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
