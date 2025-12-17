package com.revenuecat.sample.admob.ui

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView as AdMobNativeAdView
import com.revenuecat.sample.admob.R

/**
 * Composable for displaying AdMob native ads using a proper NativeAdView.
 *
 * This demonstrates:
 * - Using AdMob's NativeAdView to properly register click actions
 * - How to inflate and populate a native ad layout
 * - Integration with RevenueCat event tracking
 *
 * Native ad events (Loaded, Displayed, Opened, Revenue) are tracked
 * by the AdMobManager.
 *
 * @param nativeAd The loaded NativeAd to display
 */
@Composable
fun NativeAdView(nativeAd: NativeAd) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { context ->
            val adView = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_layout, null) as AdMobNativeAdView

            // Populate the native ad view with ad data
            populateNativeAdView(nativeAd, adView)

            adView
        }
    )
}

/**
 * Populate the AdMob NativeAdView with data from the NativeAd.
 * This properly registers all clickable elements.
 */
private fun populateNativeAdView(nativeAd: NativeAd, adView: AdMobNativeAdView) {
    // Find views by ID
    val headlineView = adView.findViewById<android.widget.TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<android.widget.TextView>(R.id.ad_body)
    val callToActionView = adView.findViewById<android.widget.Button>(R.id.ad_call_to_action)
    val iconView = adView.findViewById<android.widget.ImageView>(R.id.ad_icon)
    val mediaView = adView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)
    val advertiserView = adView.findViewById<android.widget.TextView>(R.id.ad_advertiser)

    // Set the headline (REQUIRED - must always be set)
    headlineView?.text = nativeAd.headline ?: ""
    adView.headlineView = headlineView

    // Set the call to action (REQUIRED - must always be set)
    callToActionView?.text = nativeAd.callToAction ?: "Learn More"
    callToActionView?.visibility = android.view.View.VISIBLE
    adView.callToActionView = callToActionView

    // Set the body (optional)
    if (nativeAd.body == null) {
        bodyView?.visibility = android.view.View.GONE
    } else {
        bodyView?.visibility = android.view.View.VISIBLE
        bodyView?.text = nativeAd.body
        adView.bodyView = bodyView
    }

    // Set the icon
    if (nativeAd.icon == null) {
        iconView?.visibility = android.view.View.GONE
    } else {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = android.view.View.VISIBLE
        adView.iconView = iconView
    }

    // Set the media content
    if (nativeAd.mediaContent == null) {
        mediaView?.visibility = android.view.View.GONE
    } else {
        mediaView?.setMediaContent(nativeAd.mediaContent)
        mediaView?.visibility = android.view.View.VISIBLE
        adView.mediaView = mediaView
    }

    // Set the advertiser
    if (nativeAd.advertiser == null) {
        advertiserView?.visibility = android.view.View.GONE
    } else {
        advertiserView?.text = nativeAd.advertiser
        advertiserView?.visibility = android.view.View.VISIBLE
        adView.advertiserView = advertiserView
    }

    // Register the NativeAd with the NativeAdView
    adView.setNativeAd(nativeAd)
}
