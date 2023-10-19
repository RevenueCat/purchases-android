package com.revenuecat.purchases.utils

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.ImageRequest
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.debugLog

private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB

class OfferingImagePreDownloader(
    private val applicationContext: Context,
) {
    private val shouldPredownloadImages: Boolean = try {
        Class.forName("com.revenuecat.purchases.ui.revenuecatui.PaywallKt")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    fun preDownloadOfferingImages(offering: Offering) {
        if (!shouldPredownloadImages) return
        offering.paywall?.let { paywallData ->
            val imageUris = paywallData.config.images.all.map {
                Uri.parse(paywallData.assetBaseURL.toString()).buildUpon().path(it).build()
            }
            imageUris.forEach {
                debugLog("Pre-downloading paywall image: $it")
                val request = ImageRequest.Builder(applicationContext)
                    .data(it)
                    .build()
                applicationContext.getRevenueCatUIImageLoader().enqueue(request)
            }
        }
    }
}

/**
 * This downloads paywall images in a specific cache for RevenueCat.
 * If you update this, make sure the version in the [RemoteImage] composable is also updated.
 */
private fun Context.getRevenueCatUIImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("revenuecatui_cache"))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .build()
}
