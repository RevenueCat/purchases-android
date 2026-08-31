package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache

internal object PaywallImageLoader {

    private const val MAX_CACHE_SIZE_BYTES = 25L * 1024 * 1024
    private const val CACHE_FOLDER = "revenuecatui_cache"

    private var cached: ImageLoader? = null

    @Synchronized
    fun get(context: Context): ImageLoader = cached ?: ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(CACHE_FOLDER))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .build()
        .also { cached = it }
}
