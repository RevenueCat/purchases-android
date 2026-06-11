package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.addMargin
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests ensuring that zero margin (whether from an absent JSON key or explicit zeros)
 * produces the same computed size as when no margin is applied.
 */
@RunWith(AndroidJUnit4::class)
internal class SizeAddMarginTests {

    private val zeroMargin = PaddingValues(0.dp)
    private val nonZeroMargin = PaddingValues(start = 5.dp, top = 10.dp, end = 5.dp, bottom = 10.dp)

    // --- Zero margin leaves every SizeConstraint variant unchanged ---

    @Test
    fun `Fill width and Fill height are unchanged by zero margin`() {
        val size = Size(width = Fill, height = Fill)
        val result = size.addMargin(zeroMargin, LayoutDirection.Ltr)
        assert(result == size) { "Expected $size but got $result" }
    }

    @Test
    fun `Fit width and Fit height are unchanged by zero margin`() {
        val size = Size(width = Fit, height = Fit)
        val result = size.addMargin(zeroMargin, LayoutDirection.Ltr)
        assert(result == size) { "Expected $size but got $result" }
    }

    @Test
    fun `Fixed width and Fixed height are unchanged by zero margin`() {
        val size = Size(width = Fixed(200u), height = Fixed(100u))
        val result = size.addMargin(zeroMargin, LayoutDirection.Ltr)
        assert(result == size) { "Expected $size but got $result" }
    }

    @Test
    fun `Fill width and Fit height are unchanged by zero margin`() {
        val size = Size(width = Fill, height = Fit)
        val result = size.addMargin(zeroMargin, LayoutDirection.Ltr)
        assert(result == size) { "Expected $size but got $result" }
    }

    // --- Zero margin and explicit-zero PaddingValues produce the same result ---

    @Test
    fun `Absent margin (zero) and explicit zero PaddingValues produce identical size for Fixed`() {
        val size = Size(width = Fixed(300u), height = Fixed(150u))
        val fromAbsent = size.addMargin(PaddingValues(0.dp), LayoutDirection.Ltr)
        val fromExplicit = size.addMargin(
            PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
            LayoutDirection.Ltr,
        )
        assert(fromAbsent == fromExplicit) {
            "Expected identical sizes: absent=$fromAbsent, explicit=$fromExplicit"
        }
        assert(fromAbsent == size) { "Zero margin should not change a Fixed size, got $fromAbsent" }
    }

    @Test
    fun `Absent margin (zero) and explicit zero PaddingValues produce identical size for Fill`() {
        val size = Size(width = Fill, height = Fill)
        val fromAbsent = size.addMargin(PaddingValues(0.dp), LayoutDirection.Ltr)
        val fromExplicit = size.addMargin(
            PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
            LayoutDirection.Ltr,
        )
        assert(fromAbsent == fromExplicit) {
            "Expected identical sizes: absent=$fromAbsent, explicit=$fromExplicit"
        }
    }

    // --- Non-zero margin only expands Fixed, never Fill or Fit ---

    @Test
    fun `Fill dimensions are unaffected by non-zero margin`() {
        val size = Size(width = Fill, height = Fill)
        val result = size.addMargin(nonZeroMargin, LayoutDirection.Ltr)
        assert(result == size) { "Fill should not be expanded by margin, got $result" }
    }

    @Test
    fun `Fit dimensions are unaffected by non-zero margin`() {
        val size = Size(width = Fit, height = Fit)
        val result = size.addMargin(nonZeroMargin, LayoutDirection.Ltr)
        assert(result == size) { "Fit should not be expanded by margin, got $result" }
    }

    @Test
    fun `Fixed dimensions are expanded by non-zero margin`() {
        val size = Size(width = Fixed(200u), height = Fixed(100u))
        val result = size.addMargin(nonZeroMargin, LayoutDirection.Ltr)
        // horizontal: start 5dp + end 5dp = 10dp; vertical: top 10dp + bottom 10dp = 20dp
        assert(result.width is Fixed) { "Width should remain Fixed" }
        assert(result.height is Fixed) { "Height should remain Fixed" }
        assert((result.width as Fixed).value == 210u) {
            "Expected width 210dp, got ${(result.width as Fixed).value}"
        }
        assert((result.height as Fixed).value == 120u) {
            "Expected height 120dp, got ${(result.height as Fixed).value}"
        }
    }
}
