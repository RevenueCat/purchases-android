package com.revenuecat.sample.vanilla.ui.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.sample.vanilla.R
import com.revenuecat.sample.vanilla.data.Constants
import com.google.android.gms.ads.nativead.NativeAdView as AdMobNativeAdView

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun NativeAdContent() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Not Loaded") }
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        onDispose { nativeAdState?.destroy() }
    }

    Text(
        text = "Integrated content ad (text + images). Tracks: Loaded, Displayed, Opened (on click), Revenue.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = "Note: Google's test native ad IDs have known issues. " +
            "The implementation is correct - use your own AdMob ad unit ID for testing.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )

    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = {
            status = "Loading..."
            var latestResponseInfo: ResponseInfo? = null
            val adTracker = Purchases.sharedInstance.adTracker

            AdLoader.Builder(context, Constants.AdMob.NATIVE_AD_UNIT_ID)
                .forNativeAd { nativeAd ->
                    latestResponseInfo = nativeAd.responseInfo
                    val responseInfo = latestResponseInfo ?: return@forNativeAd
                    adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.NATIVE,
                            placement = "home_native",
                            adUnitId = Constants.AdMob.NATIVE_AD_UNIT_ID,
                            impressionId = responseInfo.responseId.orEmpty(),
                        ),
                    )
                    nativeAd.setOnPaidEventListener { adValue ->
                        val ri = latestResponseInfo ?: return@setOnPaidEventListener
                        adTracker.trackAdRevenue(
                            AdRevenueData(
                                networkName = ri.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native",
                                adUnitId = Constants.AdMob.NATIVE_AD_UNIT_ID,
                                impressionId = ri.responseId.orEmpty(),
                                revenueMicros = adValue.valueMicros,
                                currency = adValue.currencyCode,
                                precision = adValue.precisionType.toAdRevenuePrecision(),
                            ),
                        )
                    }
                    nativeAdState?.destroy()
                    nativeAdState = nativeAd
                    status = "Loaded"
                    Toast.makeText(context, "Native ad loaded!", Toast.LENGTH_SHORT).show()
                }
                .withAdListener(object : AdListener() {
                    override fun onAdImpression() {
                        val responseInfo = latestResponseInfo ?: return
                        adTracker.trackAdDisplayed(
                            AdDisplayedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native",
                                adUnitId = Constants.AdMob.NATIVE_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdClicked() {
                        val responseInfo = latestResponseInfo ?: return
                        adTracker.trackAdOpened(
                            AdOpenedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native",
                                adUnitId = Constants.AdMob.NATIVE_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adTracker.trackAdFailedToLoad(
                            AdFailedToLoadData(
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native",
                                adUnitId = Constants.AdMob.NATIVE_AD_UNIT_ID,
                                mediatorErrorCode = error.code,
                            ),
                        )
                        status = "Failed: ${error.message}"
                        Toast.makeText(context, "Failed to load native ad", Toast.LENGTH_SHORT).show()
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
                .loadAd(AdRequest.Builder().build())
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !status.contains("Loading"),
    ) {
        Text("Load")
    }

    nativeAdState?.let { nativeAd ->
        NativeAdView(nativeAd = nativeAd)
    }
}

@Suppress("MultipleEmitters")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun NativeVideoAdContent() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Not Loaded") }
    var nativeVideoAdState by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        onDispose { nativeVideoAdState?.destroy() }
    }

    Text(
        text = "Integrated content ad with video. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = "Note: Google's test native ad IDs have known issues. " +
            "The implementation is correct - use your own AdMob ad unit ID for testing.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )

    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = {
            status = "Loading..."
            var latestResponseInfo: ResponseInfo? = null
            val adTracker = Purchases.sharedInstance.adTracker

            AdLoader.Builder(context, Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID)
                .forNativeAd { nativeAd ->
                    latestResponseInfo = nativeAd.responseInfo
                    val responseInfo = latestResponseInfo ?: return@forNativeAd
                    adTracker.trackAdLoaded(
                        AdLoadedData(
                            networkName = responseInfo.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.NATIVE,
                            placement = "home_native_video",
                            adUnitId = Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                            impressionId = responseInfo.responseId.orEmpty(),
                        ),
                    )
                    nativeAd.setOnPaidEventListener { adValue ->
                        val ri = latestResponseInfo ?: return@setOnPaidEventListener
                        adTracker.trackAdRevenue(
                            AdRevenueData(
                                networkName = ri.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native_video",
                                adUnitId = Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                                impressionId = ri.responseId.orEmpty(),
                                revenueMicros = adValue.valueMicros,
                                currency = adValue.currencyCode,
                                precision = adValue.precisionType.toAdRevenuePrecision(),
                            ),
                        )
                    }
                    nativeVideoAdState?.destroy()
                    nativeVideoAdState = nativeAd
                    status = "Loaded"
                    Toast.makeText(context, "Native video ad loaded!", Toast.LENGTH_SHORT).show()
                }
                .withAdListener(object : AdListener() {
                    override fun onAdImpression() {
                        val responseInfo = latestResponseInfo ?: return
                        adTracker.trackAdDisplayed(
                            AdDisplayedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native_video",
                                adUnitId = Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdClicked() {
                        val responseInfo = latestResponseInfo ?: return
                        adTracker.trackAdOpened(
                            AdOpenedData(
                                networkName = responseInfo.mediationAdapterClassName,
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native_video",
                                adUnitId = Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                                impressionId = responseInfo.responseId.orEmpty(),
                            ),
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adTracker.trackAdFailedToLoad(
                            AdFailedToLoadData(
                                mediatorName = AdMediatorName.AD_MOB,
                                adFormat = AdFormat.NATIVE,
                                placement = "home_native_video",
                                adUnitId = Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                                mediatorErrorCode = error.code,
                            ),
                        )
                        status = "Failed: ${error.message}"
                        Toast.makeText(context, "Failed to load native video ad", Toast.LENGTH_SHORT).show()
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setVideoOptions(
                            VideoOptions.Builder()
                                .setStartMuted(true)
                                .build(),
                        )
                        .build(),
                )
                .build()
                .loadAd(AdRequest.Builder().build())
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !status.contains("Loading"),
    ) {
        Text("Load")
    }

    nativeVideoAdState?.let { nativeAd ->
        NativeAdView(nativeAd = nativeAd)
    }
}

@Composable
private fun NativeAdView(nativeAd: NativeAd) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { context ->
            val adView = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_layout, null) as AdMobNativeAdView

            populateNativeAdView(nativeAd, adView)

            adView
        },
    )
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: AdMobNativeAdView) {
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)

    headlineView?.text = nativeAd.headline ?: ""
    adView.headlineView = headlineView

    callToActionView?.text = nativeAd.callToAction ?: "Learn More"
    callToActionView?.visibility = View.VISIBLE
    adView.callToActionView = callToActionView

    if (nativeAd.body == null) {
        bodyView?.visibility = View.GONE
    } else {
        bodyView?.visibility = View.VISIBLE
        bodyView?.text = nativeAd.body
        adView.bodyView = bodyView
    }

    if (nativeAd.icon == null) {
        iconView?.visibility = View.GONE
    } else {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
        adView.iconView = iconView
    }

    if (nativeAd.mediaContent == null) {
        mediaView?.visibility = View.GONE
    } else {
        mediaView?.setMediaContent(nativeAd.mediaContent)
        mediaView?.visibility = View.VISIBLE
        adView.mediaView = mediaView
    }

    if (nativeAd.advertiser == null) {
        advertiserView?.visibility = View.GONE
    } else {
        advertiserView?.text = nativeAd.advertiser
        advertiserView?.visibility = View.VISIBLE
        adView.advertiserView = advertiserView
    }

    adView.setNativeAd(nativeAd)
}
