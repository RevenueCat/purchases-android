package com.revenuecat.purchases.utils

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.ImageRequest

// Note: these values have to match those in RemoteImage
private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
private const val PAYWALL_IMAGE_CACHE_FOLDER = "revenuecatui_cache"

internal class CoilImageDownloader(
    private val applicationContext: Context,
) {
    fun downloadImage(uri: Uri) {
        val request = ImageRequest.Builder(applicationContext)
            .data(uri)
            .build()
        applicationContext.getRevenueCatUIImageLoader().enqueue(request)
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
                .directory(cacheDir.resolve(PAYWALL_IMAGE_CACHE_FOLDER))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .build()
}
