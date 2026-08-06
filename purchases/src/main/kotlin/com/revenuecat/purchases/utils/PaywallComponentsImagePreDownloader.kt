package com.revenuecat.purchases.utils

import com.revenuecat.purchases.common.canUsePaywallUI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog

internal class PaywallComponentsImagePreDownloader(
    /**
     * We check for the existence of the paywalls SDK. If so, the Coil SDK should be available to
     * pre-download the images.
     */
    private val shouldPredownloadImages: Boolean = canUsePaywallUI,
    private val coilImageDownloader: CoilImageDownloader,
) {

    fun preDownloadImages(assets: PaywallComponentAssets) {
        if (!shouldPredownloadImages) {
            verboseLog { "PaywallComponentsImagePreDownloader won't pre-download images" }
            return
        }

        assets.imageUris.forEach {
            debugLog { "Pre-downloading Paywall V2 image: $it" }
            coilImageDownloader.downloadImage(it)
        }
    }
}
