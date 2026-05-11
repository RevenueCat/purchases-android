@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.canUsePaywallUI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog

internal class OfferingImagePreDownloader(
    /**
     * We check for the existance of the paywalls SDK. If so, the Coil SDK should be available to
     * pre-download the images.
     */
    private val shouldPredownloadImages: Boolean = canUsePaywallUI,
    private val coilImageDownloader: CoilImageDownloader,
    private val paywallComponentsImagePreDownloader: PaywallComponentsImagePreDownloader =
        PaywallComponentsImagePreDownloader(shouldPredownloadImages, coilImageDownloader),
) {
    fun preDownloadOfferingImages(offering: Offering) {
        if (!shouldPredownloadImages) {
            verboseLog { "OfferingImagePreDownloader won't pre-download images" }
            return
        }

        debugLog { "OfferingImagePreDownloader: starting image download" }

        downloadV1Images(offering)
        downloadV2Images(offering)
    }

    private fun downloadV1Images(offering: Offering) {
        offering.paywall?.let { paywallData ->
            val imageUris = paywallData.config.images.all.map {
                Uri.parse(paywallData.assetBaseURL.toString()).buildUpon().path(it).build()
            }
            imageUris.forEach {
                debugLog { "Pre-downloading Paywall V1 image: $it" }
                coilImageDownloader.downloadImage(it)
            }
        }
    }

    private fun downloadV2Images(offering: Offering) {
        offering.paywallComponents?.let { paywallComponents ->
            paywallComponentsImagePreDownloader.preDownloadImages(
                paywallComponentsConfig = paywallComponents.data.componentsConfig.base,
            )
        }
    }
}
