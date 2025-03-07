package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.Context
import coil.ImageLoader
import com.revenuecat.purchases.Purchases

internal fun Purchases.Companion.getImageLoaderTyped(context: Context): ImageLoader {
    return Purchases.getImageLoader(context) as ImageLoader
}
