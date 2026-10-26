@file:Suppress("LongMethod")

package com.revenuecat.sample.vanilla.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener
import kotlin.math.roundToLong

private const val PLACEMENT = "home_banner"

@Suppress("MultipleEmitters")
@Composable
internal fun BannerAdContent() {
    var status by remember { mutableStateOf("Loading…") }
    var bannerAdView by remember { mutableStateOf<LevelPlayBannerAdView?>(null) }

    DisposableEffect(Unit) {
        onDispose { bannerAdView?.destroy() }
    }

    Text(
        text = "Auto-loaded banner. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            val config = LevelPlayBannerAdView.Config.Builder()
                .setAdSize(LevelPlayAdSize.BANNER)
                .setPlacementName(PLACEMENT)
                .build()
            LevelPlayBannerAdView(context, Constants.LevelPlay.BANNER_AD_UNIT_ID, config).apply {
                setBannerListener(object : LevelPlayBannerAdViewListener {
                    override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                        Purchases.sharedInstance.adTracker.trackAdLoaded(
                            AdLoadedData(
                                networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                                mediatorName = AdMediatorName.LEVEL_PLAY,
                                adFormat = AdFormat.BANNER,
                                placement = PLACEMENT,
                                adUnitId = Constants.LevelPlay.BANNER_AD_UNIT_ID,
                                impressionId = adInfo.auctionId.orEmpty(),
                            ),
                        )
                        status = "Loaded"
                    }

                    override fun onAdLoadFailed(error: LevelPlayAdError) {
                        Purchases.sharedInstance.adTracker.trackAdFailedToLoad(
                            AdFailedToLoadData(
                                mediatorName = AdMediatorName.LEVEL_PLAY,
                                adFormat = AdFormat.BANNER,
                                placement = PLACEMENT,
                                adUnitId = Constants.LevelPlay.BANNER_AD_UNIT_ID,
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
                                adFormat = AdFormat.BANNER,
                                placement = PLACEMENT,
                                adUnitId = Constants.LevelPlay.BANNER_AD_UNIT_ID,
                                impressionId = adInfo.auctionId.orEmpty(),
                            ),
                        )
                        adTracker.trackAdRevenue(
                            AdRevenueData(
                                networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                                mediatorName = AdMediatorName.LEVEL_PLAY,
                                adFormat = AdFormat.BANNER,
                                placement = PLACEMENT,
                                adUnitId = Constants.LevelPlay.BANNER_AD_UNIT_ID,
                                impressionId = adInfo.auctionId.orEmpty(),
                                revenueMicros = (adInfo.revenue * 1_000_000).roundToLong(),
                                currency = "USD",
                                precision = adInfo.precision.toAdRevenuePrecision(),
                            ),
                        )
                        status = "Displayed"
                    }

                    override fun onAdDisplayFailed(adInfo: LevelPlayAdInfo, error: LevelPlayAdError) {
                        status = "Display failed: ${error.errorMessage}"
                    }

                    override fun onAdClicked(adInfo: LevelPlayAdInfo) {
                        Purchases.sharedInstance.adTracker.trackAdOpened(
                            AdOpenedData(
                                networkName = adInfo.adNetwork.takeIf { it.isNotBlank() },
                                mediatorName = AdMediatorName.LEVEL_PLAY,
                                adFormat = AdFormat.BANNER,
                                placement = PLACEMENT,
                                adUnitId = Constants.LevelPlay.BANNER_AD_UNIT_ID,
                                impressionId = adInfo.auctionId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdExpanded(adInfo: LevelPlayAdInfo) = Unit
                    override fun onAdCollapsed(adInfo: LevelPlayAdInfo) = Unit
                    override fun onAdLeftApplication(adInfo: LevelPlayAdInfo) = Unit
                })
                bannerAdView = this
                loadAd()
            }
        },
    )
}
