package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache

internal object PaywallImageLoader {

    private const val MAX_CACHE_SIZE_BYTES = 25L * 1024 * 1024
    private const val CACHE_FOLDER = "revenuecatui_cache"

    private val affectedBrands = setOf("oneplus", "oppo", "realme")

    private var cached: ImageLoader? = null

    @Synchronized
    fun get(context: Context): ImageLoader = cached ?: ImageLoader.Builder(context)
        .allowHardware(allowHardwareBitmaps(Build.VERSION.SDK_INT, Build.BRAND, Build.MANUFACTURER))
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(CACHE_FOLDER))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .build()
        .also { cached = it }

    // Oplus Android 16 firmware can abort during hardware bitmap allocation (RevenueCat/purchases-android#4641).
    internal fun allowHardwareBitmaps(sdk: Int, brand: String, manufacturer: String): Boolean =
        sdk != Build.VERSION_CODES.BAKLAVA ||
            (brand.lowercase() !in affectedBrands && manufacturer.lowercase() !in affectedBrands)
}
