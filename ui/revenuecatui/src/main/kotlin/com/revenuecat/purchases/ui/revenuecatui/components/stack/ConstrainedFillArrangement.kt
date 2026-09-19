@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.LayoutDirection
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import kotlin.math.roundToInt

internal fun MeasureScope.arrangeConstrainedFillItems(
    config: ConstrainedFillLayout.Config,
    totalSize: Int,
    sizes: IntArray,
    spacing: Int,
): IntArray {
    if (config.distribution.usesAllAvailableSpace) {
        return flexibleSpacePositions(
            totalSize = totalSize,
            sizes = sizes,
            spacing = spacing,
            distribution = config.distribution,
            reverseInput = config.orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl,
        )
    }

    return IntArray(sizes.size).also { positions ->
        when (config) {
            is ConstrainedFillLayout.Config.Horizontal -> with(config.arrangement) {
                arrange(totalSize, sizes, layoutDirection, positions)
            }
            is ConstrainedFillLayout.Config.Vertical -> with(config.arrangement) {
                arrange(totalSize, sizes, positions)
            }
        }
    }
}

private fun flexibleSpacePositions(
    totalSize: Int,
    sizes: IntArray,
    spacing: Int,
    distribution: FlexDistribution,
    reverseInput: Boolean,
): IntArray {
    val positions = IntArray(sizes.size)
    if (sizes.isEmpty()) return positions

    val totalSpacing = spacing * (sizes.size - 1).coerceAtLeast(0)
    val remainingSpace = (totalSize - sizes.sum() - totalSpacing).coerceAtLeast(0)
    val (edgeSpace, interItemSpace) = when (distribution) {
        FlexDistribution.SPACE_BETWEEN -> {
            0f to if (sizes.size > 1) remainingSpace.toFloat() / (sizes.size - 1) else 0f
        }
        FlexDistribution.SPACE_AROUND -> {
            val unit = remainingSpace.toFloat() / (sizes.size * 2)
            unit to unit * 2
        }
        FlexDistribution.SPACE_EVENLY -> {
            val unit = remainingSpace.toFloat() / (sizes.size + 1)
            unit to unit
        }
        else -> error("Expected a distribution that uses all available space, but was $distribution.")
    }

    val indices = if (reverseInput) sizes.indices.reversed() else sizes.indices
    var currentPosition = if (
        reverseInput &&
        sizes.size == 1 &&
        distribution == FlexDistribution.SPACE_BETWEEN
    ) {
        remainingSpace.toFloat()
    } else {
        edgeSpace
    }
    indices.forEachIndexed { position, index ->
        positions[index] = currentPosition.roundToInt()
        currentPosition += sizes[index]
        if (position < sizes.lastIndex) {
            currentPosition += spacing + interItemSpace
        }
    }
    return positions
}
