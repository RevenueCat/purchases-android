package com.revenuecat.sample.admob.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.revenuecat.sample.admob.manager.AdMobManager

/**
 * Composable wrapper for displaying AdMob banner ads.
 *
 * This demonstrates:
 * - How to integrate AdView with Jetpack Compose using AndroidView
 * - How to trigger ad loading with RevenueCat event tracking
 *
 * All banner ad events (Loaded, Displayed, Opened, Revenue, FailedToLoad)
 * are automatically tracked by the AdMobManager.
 *
 * @param adSize The banner ad size (e.g., AdSize.BANNER, AdSize.LARGE_BANNER)
 * @param adUnitId The AdMob ad unit ID
 * @param adMobManager The manager instance for tracking events
 * @param placement Placement identifier for analytics
 */
@Composable
fun BannerAdView(
    adSize: AdSize = AdSize.BANNER,
    adUnitId: String,
    adMobManager: AdMobManager,
    placement: String
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId

                // Load the ad with event tracking
                adMobManager.loadBannerAd(
                    adView = this,
                    adUnitId = adUnitId,
                    placement = placement
                )
            }
        }
    )
}
