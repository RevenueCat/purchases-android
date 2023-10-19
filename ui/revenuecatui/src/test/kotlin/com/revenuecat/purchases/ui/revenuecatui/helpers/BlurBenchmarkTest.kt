package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import blur
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlurBenchmarkTest {
    @Ignore("Test is for checking performance only")
    @Test
    fun `Blur images using native blur`() = runBlocking {
        benchmarkBlur("Native Blur") { image, context, radius ->
            image.blur(context = context, radius = radius)
        }
    }

    private suspend fun benchmarkBlur(functionName: String, blurFunction: suspend (Bitmap, Context, Float) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = openImage()
        val radius = 25f
        val iterations = 100
        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            blurFunction(image, context, radius)
        }
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime / iterations
        Log.d(TAG, "$functionName:\nTotal time: $totalTime ms, Average time: $averageTime ms")
    }

    private fun openImage(): Bitmap {
        val inputStream = javaClass.classLoader!!.getResource("sample_blurring_image.jpeg").openStream()
        return BitmapFactory.decodeStream(inputStream)
    }
}
