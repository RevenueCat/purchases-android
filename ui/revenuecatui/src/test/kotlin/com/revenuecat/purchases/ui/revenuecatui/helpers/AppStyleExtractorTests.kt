package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppStyleExtractorTests {

    @Test
    fun `extractProminentColors returns empty list for null bitmap`() = runTest {
        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = null,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors returns colors for valid bitmap with red`() = runTest {
        val bitmap = createTestBitmap(AndroidColor.rgb(200, 50, 50))
        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = bitmap,
            count = 2,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).isNotEmpty()
    }

    @Test
    fun `extractProminentColors returns colors for large bitmap`() = runTest {
        val bitmap = createTestBitmap(
            color = AndroidColor.rgb(200, 50, 50),
            width = 400,
            height = 400,
        )
        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = bitmap,
            count = 1,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).hasSize(1)
    }

    @Test
    fun `extractProminentColors filters out near-black colors`() = runTest {
        val bitmap = createTestBitmap(AndroidColor.BLACK)
        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = bitmap,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors filters out near-white colors`() = runTest {
        val bitmap = createTestBitmap(AndroidColor.WHITE)
        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = bitmap,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors filters out transparent pixels`() = runTest {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Leave the bitmap with default transparent pixels
        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = bitmap,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors returns at most the requested count`() = runTest {
        // Create a bitmap with multiple distinct colors
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Fill top half with red
        for (x in 0 until 100) {
            for (y in 0 until 50) {
                bitmap.setPixel(x, y, AndroidColor.rgb(200, 50, 50))
            }
        }
        // Fill bottom half with blue
        for (x in 0 until 100) {
            for (y in 50 until 100) {
                bitmap.setPixel(x, y, AndroidColor.rgb(50, 50, 200))
            }
        }

        val colors = AppStyleExtractor.getProminentColorsFromBitmap(
            bitmap = bitmap,
            count = 1,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        assertThat(colors).hasSize(1)
    }

    @Test
    fun `colorDistance calculates correct distance between black and white`() {
        val black = Triple(0.0, 0.0, 0.0)
        val white = Triple(1.0, 1.0, 1.0)
        val distance = colorDistance(black, white)
        // sqrt(3) ≈ 1.732
        assertThat(distance).isBetween(1.73, 1.74)
    }

    @Test
    fun `colorDistance returns zero for same colors`() {
        val color = Triple(0.5, 0.5, 0.5)
        val distance = colorDistance(color, color)
        assertThat(distance).isEqualTo(0.0)
    }

    @Test
    fun `contrastRatio returns correct ratio for black and white`() {
        val ratio = contrastRatio(Color.Black, Color.White)
        // Should be 21:1
        assertThat(ratio).isBetween(20.9, 21.1)
    }

    @Test
    fun `contrastRatio returns 1 for same colors`() {
        val ratio = contrastRatio(Color.Red, Color.Red)
        assertThat(ratio).isEqualTo(1.0)
    }

    @Test
    fun `relativeLuminance returns 0 for black`() {
        val luminance = relativeLuminance(Color.Black)
        assertThat(luminance).isEqualTo(0.0)
    }

    @Test
    fun `relativeLuminance returns 1 for white`() {
        val luminance = relativeLuminance(Color.White)
        assertThat(luminance).isEqualTo(1.0)
    }

    @Test
    fun `selectColorWithBestContrast selects white against black background`() {
        val colors = listOf(Color.White, Color.Gray, Color.DarkGray)
        val selected = selectColorWithBestContrast(colors, Color.Black)
        assertThat(selected).isEqualTo(Color.White)
    }

    @Test
    fun `selectColorWithBestContrast selects black against white background`() {
        val colors = listOf(Color.Black, Color.Gray, Color.LightGray)
        val selected = selectColorWithBestContrast(colors, Color.White)
        assertThat(selected).isEqualTo(Color.Black)
    }

    @Test
    fun `selectColorWithBestContrast returns null for empty list`() {
        val colors = emptyList<Color>()
        val selected = selectColorWithBestContrast(colors, Color.Red)
        assertThat(selected).isNull()
    }

    private fun createTestBitmap(
        color: Int,
        width: Int = 100,
        height: Int = 100,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
}
