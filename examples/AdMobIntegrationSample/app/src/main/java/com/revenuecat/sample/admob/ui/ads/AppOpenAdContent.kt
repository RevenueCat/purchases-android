@file:OptIn(com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.sample.admob.ui.ads

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RCAdMob
import com.revenuecat.sample.admob.data.Constants

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun AppOpenAdContent(activity: Activity) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Not Loaded") }
    var appOpenAd by remember { mutableStateOf<AppOpenAd?>(null) }

    Text(
        text = "Full-screen ad designed for app launch/resume. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
        style = MaterialTheme.typography.bodySmall,
    )

    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
    )

    Button(
        onClick = {
            status = "Loading..."
            RCAdMob.loadAndTrackAppOpenAd(
                context = context,
                adUnitId = Constants.AdMob.APP_OPEN_AD_UNIT_ID,
                adRequest = AdRequest.Builder().build(),
                placement = "home_app_open",
                loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        status = "Loaded - Ready to Show"
                        Toast.makeText(context, "App open ad loaded!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        status = "Failed: ${error.message}"
                        Toast.makeText(context, "Failed to load", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Load")
    }

    Button(
        onClick = {
            val ad = appOpenAd
            if (ad != null) {
                ad.show(activity)
                appOpenAd = null
                status = "Shown - Load Again"
            } else {
                Toast.makeText(context, "No ad loaded yet", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = status.contains("Loaded"),
    ) {
        Text("Show")
    }
}
