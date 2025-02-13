@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

// Note: these values have to match those in CoilImageDownloader
private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
private const val PAYWALL_IMAGE_CACHE_FOLDER = "revenuecatui_cache"

/**
 * We hold a singleton reference to the ImageLoader to avoid creating multiple instances.
 * The ImageLoader only holds a reference to the application context, so we shouldn't leak anything.
 */
private var cachedImageLoader: ImageLoader? = null

/**
 * This downloads paywall images in a specific cache for RevenueCat.
 * If you update this, make sure the version in the [CoilImageDownloader] class is also updated.
 */
@JvmSynthetic
internal fun Context.getRevenueCatUIImageLoader(): ImageLoader {
    return synchronized(Unit) {
        val currentImageLoader = cachedImageLoader
        if (currentImageLoader == null) {
            val newImageLoader = ImageLoader.Builder(this)
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
                .build()
            cachedImageLoader = newImageLoader
            newImageLoader
        } else {
            currentImageLoader
        }
    }
}
