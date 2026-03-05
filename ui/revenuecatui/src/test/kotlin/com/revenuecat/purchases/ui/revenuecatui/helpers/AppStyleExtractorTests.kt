package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.ui.revenuecatui.defaultpaywall.DualColorImageGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppStyleExtractorTests {

    @Test
    fun `extractProminentColors returns empty list for null bitmap`() {
        val colors = AppStyleExtractor.extractProminentColorsSync(null)
        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors extracts red and green from red-green image`() {
        val bitmap = DualColorImageGenerator.redGreen.bitmap
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap)

        assertThat(colors).hasSize(2)
        assertThat(colors.any { isColorNear(it, Color.Red) }).isTrue()
        assertThat(colors.any { isColorNear(it, Color.Green) }).isTrue()
    }

    @Test
    fun `extractProminentColors extracts blue and green from blue-green image`() {
        val bitmap = DualColorImageGenerator.blueGreen.bitmap
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap)

        assertThat(colors).hasSize(2)
        assertThat(colors.any { isColorNear(it, Color.Blue) }).isTrue()
        assertThat(colors.any { isColorNear(it, Color.Green) }).isTrue()
    }

    @Test
    fun `extractProminentColors extracts purple and orange from purple-orange image`() {
        val bitmap = DualColorImageGenerator.purpleOrange.bitmap
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap)

        val orange = Color(0xFFFFA500)
        assertThat(colors).hasSize(2)
        assertThat(colors.any { isColorNear(it, Color(0xFF800080)) }).isTrue()
        assertThat(colors.any { isColorNear(it, orange) }).isTrue()
    }

    @Test
    fun `extractProminentColors returns distinct colors`() {
        val bitmap = DualColorImageGenerator.redGreen.bitmap
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap)

        assertThat(colors).hasSizeGreaterThanOrEqualTo(2)
        for (index in 0 until colors.lastIndex) {
            for (index2 in index + 1..colors.lastIndex) {
                val distance = colorDistance(colors[index].toTriple(), colors[index2].toTriple())
                assertThat(distance).isGreaterThan(ColorExtractionConstants.MINIMUM_COLOR_DISTANCE)
            }
        }
    }

    @Test
    fun `extractProminentColors filters out transparent pixels`() {
        val bitmap = DualColorImageGenerator.transparent()
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap)
        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors filters out very dark and very bright colors`() {
        val bitmap = DualColorImageGenerator.blackWhite.bitmap
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap)

        assertThat(colors).isEmpty()
    }

    @Test
    fun `extractProminentColors for single color image extracts one color or less`() {
        val testColor = Color(red = 0.8f, green = 0.2f, blue = 0.2f)
        val image = DualColorImageGenerator.singleColor(testColor)
        assertThat(image).isNotNull

        val colors = AppStyleExtractor.extractProminentColorsSync(image?.bitmap)
        assertThat(colors.size).isLessThanOrEqualTo(1)
        if (colors.isNotEmpty()) {
            assertThat(isColorNear(colors.first(), testColor)).isTrue()
        }
    }

    @Test
    fun `extractProminentColors returns at most requested count`() {
        val bitmap = DualColorImageGenerator.redGreen.bitmap
        val colors = AppStyleExtractor.extractProminentColorsSync(bitmap, count = 1)
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
    fun `selectColorWithBestContrast returns background color for empty list`() {
        val colors = emptyList<Color>()
        val selected = selectColorWithBestContrast(colors, Color.Red)
        assertThat(selected).isEqualTo(Color.Red)
    }

    private fun isColorNear(actual: Color, expected: Color, tolerance: Double = 0.3): Boolean {
        return colorDistance(actual.toTriple(), expected.toTriple()) < tolerance
    }

    private fun Color.toTriple(): Triple<Double, Double, Double> {
        return Triple(red.toDouble(), green.toDouble(), blue.toDouble())
    }
}
