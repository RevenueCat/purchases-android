package com.revenuecat.purchases.ui.revenuecatui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

internal suspend fun URL.toImageBitmap(): Result<Bitmap> {
    return withContext(Dispatchers.IO) {
        try {
            val connection = openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            if (bitmap == null) {
                val exception = IOException("Image could not be decoded from url $this")
                Logger.e("Error decoding image from $this", exception)
                Result.failure(exception)
            } else {
                Result.success(bitmap)
            }
        } catch (e: IOException) {
            Logger.e("Error downloading image from $this", e)
            Result.failure(e)
        }
    }
}
