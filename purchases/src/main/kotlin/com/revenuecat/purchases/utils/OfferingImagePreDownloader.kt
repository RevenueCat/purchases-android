@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.PaywallAssetWarming

internal class OfferingImagePreDownloader(
    private val assetWarming: PaywallAssetWarming,
) {
    fun preDownloadOfferingImages(offering: Offering) {
        if (!assetWarming.isAvailable) {
            verboseLog { "OfferingImagePreDownloader won't pre-download images" }
            return
        }

        debugLog { "OfferingImagePreDownloader: starting image download" }

        downloadV1Images(offering)
        downloadV2Images(offering)
    }

    private fun downloadV1Images(offering: Offering) {
        val paywallData = offering.paywall ?: return
        val imageUris = paywallData.config.images.all.map {
            Uri.parse(paywallData.assetBaseURL.toString()).buildUpon().path(it).build()
        }
        assetWarming.warmImages(imageUris)
    }

    private fun downloadV2Images(offering: Offering) {
        val assets = offering.baseComponentsConfig()?.collectAssets() ?: return
        assetWarming.warmImages(assets.imageUris)
        // Runs before offerings are delivered, so a paywall presented from that callback does not pay WebView
        // engine startup on the UI thread. Warming the bundles themselves happens later.
        if (assets.webViewUrls.isNotEmpty()) assetWarming.prebootWebView()
    }
}
