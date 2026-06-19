package com.revenuecat.sample.vanilla.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
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

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        val responseInfo = this@apply.responseInfo ?: return
                        Purchases.sharedInstance.adTracker.trackAdLoaded(
                            AdLoadedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.BANNER,
                                placement = "home_banner",
                                adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdImpression() {
                        val responseInfo = this@apply.responseInfo ?: return
                        Purchases.sharedInstance.adTracker.trackAdDisplayed(
                            AdDisplayedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.BANNER,
                                placement = "home_banner",
                                adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdClicked() {
                        val responseInfo = this@apply.responseInfo ?: return
                        Purchases.sharedInstance.adTracker.trackAdOpened(
                            AdOpenedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.BANNER,
                                placement = "home_banner",
                                adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Purchases.sharedInstance.adTracker.trackAdFailedToLoad(
                            AdFailedToLoadData(
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.BANNER,
                                placement = "home_banner",
                                adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID,
                                mediatorErrorCode = error.code,
                            ),
                        )
                    }
                }

                setOnPaidEventListener { adValue ->
                    val responseInfo = this.responseInfo ?: return@setOnPaidEventListener
                    Purchases.sharedInstance.adTracker.trackAdRevenue(
                        AdRevenueData(
                            networkName = responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.BANNER,
                            placement = "home_banner",
                            adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID,
                            impressionId = responseInfo.responseId.orEmpty(),
                            revenueMicros = adValue.valueMicros,
                            currency = adValue.currencyCode,
                            precision = adValue.precisionType.toAdRevenuePrecision(),
                        ),
                    )
                }

                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
