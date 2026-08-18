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
        val componentsConfig = offering.baseComponentsConfig() ?: return
        assetWarming.warmImages(componentsConfig.collectAssets().imageUris)
    }
}
