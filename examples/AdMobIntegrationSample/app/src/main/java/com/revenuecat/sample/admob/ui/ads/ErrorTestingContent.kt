@file:OptIn(com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.sample.admob.ui.ads

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdRequest
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RCAdMob
import com.revenuecat.sample.admob.data.Constants

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ErrorTestingContent() {
    val context = LocalContext.current

    Text(
        text = "Uses invalid ad unit ID to trigger and track ad load failure.",
        style = MaterialTheme.typography.bodySmall,
    )

    Button(
        onClick = {
            RCAdMob.loadAndTrackInterstitialAd(
                context = context,
                adUnitId = Constants.AdMob.INVALID_AD_UNIT_ID,
                adRequest = AdRequest.Builder().build(),
                placement = "error_test",
            )
            Toast.makeText(
                context,
                "Loading with invalid ID - check logs for failure tracking",
                Toast.LENGTH_LONG,
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Trigger Ad Load Error")
    }
}
