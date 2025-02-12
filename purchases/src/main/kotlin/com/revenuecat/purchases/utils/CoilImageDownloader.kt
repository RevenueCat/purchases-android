package com.revenuecat.purchases.utils

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest

internal class CoilImageDownloader(
    private val applicationContext: Context,
) {
    fun downloadImage(uri: Uri, loader: ImageLoader) {
        val request = ImageRequest.Builder(applicationContext)
            .data(uri)
            .build()
        loader.enqueue(request)
    }
}
