@file:OptIn(com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.sample.admob.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.loadAndTrackAd
import com.revenuecat.sample.admob.data.Constants

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun BannerAdContent() {
    Text(
        text = "Always visible at the top. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
        style = MaterialTheme.typography.bodySmall,
    )

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID

                // Load the ad with RevenueCat event tracking
                loadAndTrackAd(adRequest = AdRequest.Builder().build(), placement = "home_banner")
            }
        },
    )
}
