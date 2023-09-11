package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import coil.ImageLoader
import coil.disk.DiskCache

/**
 * Returns the activity from a given context. Most times, the context itself will be
 * an activity, but in the case it's not, it will iterate through the context wrappers until it
 * finds one that is an activity.
 */
internal fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

private const val MAX_CACHE_SIZE_PERCENTAGE = 0.01

@Composable
@ReadOnlyComposable
internal fun Context.getRevenueCatUIImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("revenuecatui_cache"))
                .maxSizePercent(MAX_CACHE_SIZE_PERCENTAGE)
                .build()
        }
        .build()
}
