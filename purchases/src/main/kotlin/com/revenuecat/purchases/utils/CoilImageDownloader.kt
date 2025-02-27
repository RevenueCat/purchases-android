package com.revenuecat.purchases.utils

import android.content.Context
import android.net.Uri
import coil.request.ImageRequest
import com.revenuecat.purchases.PurchasesOrchestrator

internal class CoilImageDownloader(
    private val applicationContext: Context,
) {
    fun downloadImage(uri: Uri) {
        val loader = PurchasesOrchestrator.getImageLoader(applicationContext)
        val request = ImageRequest.Builder(applicationContext)
            .data(uri)
            .build()
        loader.enqueue(request)
    }
}
