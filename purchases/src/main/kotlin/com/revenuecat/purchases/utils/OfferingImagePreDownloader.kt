package com.revenuecat.purchases.utils

import android.net.Uri
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog

internal class OfferingImagePreDownloader(
    /**
     * We check for the existance of the paywalls SDK. If so, the Coil SDK should be available to
     * pre-download the images.
     */
    private val shouldPredownloadImages: Boolean = try {
        Class.forName("com.revenuecat.purchases.ui.revenuecatui.PaywallKt")
        true
    } catch (_: ClassNotFoundException) {
        false
    },
    private val coilImageDownloader: CoilImageDownloader,
) {

    fun preDownloadOfferingImages(offering: Offering) {
        if (!shouldPredownloadImages) {
            verboseLog("OfferingImagePreDownloader won't pre-download images")
            return
        }

        debugLog("OfferingImagePreDownloader: starting image download")

        offering.paywall?.let { paywallData ->
            val imageUris = paywallData.config.images.all.map {
                Uri.parse(paywallData.assetBaseURL.toString()).buildUpon().path(it).build()
            }
            imageUris.forEach {
                debugLog("Pre-downloading paywall image: $it")
                coilImageDownloader.downloadImage(it)
            }
        }
    }
}
