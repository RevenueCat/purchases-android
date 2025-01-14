@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution

/**
 * Converts this [FlexDistribution] to an [Arrangement.Horizontal].
 *
 * Note that for [FlexDistribution.SPACE_AROUND] and [FlexDistribution.SPACE_EVENLY], you still need to add the
 * appropriate padding before and after the content manually.
 */
@JvmSynthetic
internal fun FlexDistribution.toHorizontalArrangement(spacing: Dp): Arrangement.Horizontal =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Start)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.End)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        FlexDistribution.SPACE_BETWEEN,
        // Arrangement.Horizontal.spacing is interpreted by Row as the spacing between items when measuring its
        // size. Therefore we cannot rely on Arrangement.Horizontal alone to arrange items as SPACE_AROUND and
        // SPACE_EVENLY with a minimum spacing, as those have spacing before and after all items too. This
        // spacing is ignored by Row when measuring, causing children to be cut off.
        // We'll need to add appropriately-sized Spacers manually.
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_EVENLY,
        -> SpaceBetween(spacing)
    }

/**
 * Converts this [FlexDistribution] to an [Arrangement.Vertical].
 *
 * Note that for [FlexDistribution.SPACE_AROUND] and [FlexDistribution.SPACE_EVENLY], you still need to add the
 * appropriate padding before and after the content manually.
 */
@JvmSynthetic
internal fun FlexDistribution.toVerticalArrangement(spacing: Dp): Arrangement.Vertical =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Top)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.Bottom)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterVertically)
        FlexDistribution.SPACE_BETWEEN,
        // Arrangement.Vertical.spacing is interpreted by Column as the spacing between items when measuring its
        // size. Therefore we cannot rely on Arrangement.Vertical alone to arrange items as SPACE_AROUND and
        // SPACE_EVENLY with a minimum spacing, as those have spacing before and after all items too. This
        // spacing is ignored by Column when measuring, causing children to be cut off.
        // We'll need to add appropriately-sized Spacers manually.
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_EVENLY,
        -> SpaceBetween(spacing)
    }

/**
 * A copy of [Arrangement.SpaceBetween] with non-zero [spacing].
 */
private class SpaceBetween(
    override val spacing: Dp,
) : HorizontalOrVertical {

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) = if (layoutDirection == LayoutDirection.Ltr) {
        placeSpaceBetween(totalSize, sizes, spacing.toPx(), outPositions, reverseInput = false)
    } else {
        placeSpaceBetween(totalSize, sizes, spacing.toPx(), outPositions, reverseInput = true)
    }

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) = placeSpaceBetween(totalSize, sizes, spacing.toPx(), outPositions, reverseInput = false)

    override fun toString() = "Arrangement#SpaceBetween(spacing = [$spacing])"

    private fun placeSpaceBetween(
        totalSize: Int,
        sizes: IntArray,
        spacingPx: Float,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        if (sizes.isEmpty()) return

        val consumedSize = sizes.fold(0) { a, b -> a + b }
        val noOfGaps = maxOf(sizes.lastIndex, 1)
        val gapSize = ((totalSize - consumedSize).toFloat() / noOfGaps).coerceAtLeast(spacingPx)

        var current = 0f
        if (reverseInput && sizes.size == 1) {
            // If the layout direction is right-to-left and there is only one gap,
            // we start current with the gap size. That forces the single item to be right-aligned.
            current = gapSize
        }
        sizes.forEachIndexed(reverseInput) { index, itemSize ->
            outPosition[index] = current.fastRoundToInt()
            current += itemSize.toFloat() + gapSize
        }
    }
}

private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
    if (!reversed) {
        forEachIndexed(action)
    } else {
        for (i in (size - 1) downTo 0) {
            action(i, get(i))
        }
    }
}
