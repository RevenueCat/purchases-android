@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.ui.revenuecatui.components.stack.flexibleSpacePositions

/**
 * Converts this [FlexDistribution] to an [Arrangement.Horizontal].
 *
 * The returned arrangement applies [spacing] as a minimum gap.
 */
@JvmSynthetic
internal fun FlexDistribution.toHorizontalArrangement(spacing: Dp): Arrangement.Horizontal =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Start)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.End)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        FlexDistribution.SPACE_BETWEEN,
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_EVENLY,
        -> HorizontalFlexArrangement(distribution = this, spacing = spacing)
    }

/**
 * Converts this [FlexDistribution] to an [Arrangement.Vertical].
 *
 * The returned arrangement applies [spacing] as a minimum gap.
 */
@JvmSynthetic
internal fun FlexDistribution.toVerticalArrangement(spacing: Dp): Arrangement.Vertical =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Top)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.Bottom)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterVertically)
        FlexDistribution.SPACE_BETWEEN,
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_EVENLY,
        -> VerticalFlexArrangement(distribution = this, spacing = spacing)
    }

private class HorizontalFlexArrangement(
    private val distribution: FlexDistribution,
    override val spacing: Dp,
) : Arrangement.Horizontal {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        flexibleSpacePositions(
            totalSize = totalSize,
            sizes = sizes,
            spacing = spacing.roundToPx(),
            distribution = distribution,
            reverseInput = layoutDirection == LayoutDirection.Rtl,
        ).copyInto(outPositions)
    }
}

private class VerticalFlexArrangement(
    private val distribution: FlexDistribution,
    override val spacing: Dp,
) : Arrangement.Vertical {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) {
        flexibleSpacePositions(
            totalSize = totalSize,
            sizes = sizes,
            spacing = spacing.roundToPx(),
            distribution = distribution,
            reverseInput = false,
        ).copyInto(outPositions)
    }
}
