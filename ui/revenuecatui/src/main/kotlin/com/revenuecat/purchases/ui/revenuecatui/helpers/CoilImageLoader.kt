@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

// Note: these values have to match those in CoilImageDownloader
private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
private const val PAYWALL_IMAGE_CACHE_FOLDER = "revenuecatui_cache"

/**
 * This downloads paywall images in a specific cache for RevenueCat.
 * If you update this, make sure the version in the [CoilImageDownloader] class is also updated.
 *
 * @param readCache: set to false to ignore cache for reading, but allow overwriting with updated image.
 */
@JvmSynthetic
internal fun Context.getRevenueCatUIImageLoader(readCache: Boolean): ImageLoader {
    val cachePolicy = if (readCache) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY

    return ImageLoader.Builder(this)
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve(PAYWALL_IMAGE_CACHE_FOLDER))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .memoryCache(
            MemoryCache.Builder(this)
                .build(),
        )
        .diskCachePolicy(cachePolicy)
        .memoryCachePolicy(cachePolicy)
        .build()
}
