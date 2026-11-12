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
    fixedGaps: IntArray = IntArray((sizes.size - 1).coerceAtLeast(0)) { spacing },
): IntArray {
    if (config.distribution.usesAllAvailableSpace) {
        return flexibleSpacePositions(
            totalSize = totalSize,
            sizes = sizes,
            fixedGaps = fixedGaps,
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
    fixedGaps: IntArray,
    distribution: FlexDistribution,
    reverseInput: Boolean,
): IntArray {
    val positions = IntArray(sizes.size)
    if (sizes.isEmpty()) return positions

    val remainingSpace = (totalSize - sizes.sum() - fixedGaps.sum()).coerceAtLeast(0)
    val hasEdges = distribution != FlexDistribution.SPACE_BETWEEN
    val weights = distribution.spacerWeights(sizes.size)
    // Row/Column round each weighted spacer, then distribute the rounding remainder in logical order.
    val weightUnit = if (weights.isEmpty()) 0f else remainingSpace.toFloat() / weights.sum()
    val spaces = IntArray(weights.size) { (weightUnit * weights[it]).roundToInt() }
    var remainder = remainingSpace - spaces.sum()
    spaces.indices.forEach { index ->
        val correction = remainder.coerceIn(-1, 1)
        spaces[index] = (spaces[index] + correction).coerceAtLeast(0)
        remainder -= correction
    }

    var currentPosition = if (hasEdges) spaces[0] else 0
    sizes.indices.forEach { index ->
        positions[index] = if (reverseInput) totalSize - currentPosition - sizes[index] else currentPosition
        currentPosition += sizes[index]
        if (index < sizes.lastIndex) {
            currentPosition += fixedGaps[index] + spaces[index + if (hasEdges) 1 else 0]
        }
    }
    return positions
}

private fun FlexDistribution.spacerWeights(childCount: Int): IntArray = when (this) {
    FlexDistribution.SPACE_BETWEEN -> IntArray((childCount - 1).coerceAtLeast(0)) { 1 }
    FlexDistribution.SPACE_AROUND -> IntArray(childCount + 1) {
        if (it == 0 || it == childCount) 1 else 2
    }
    FlexDistribution.SPACE_EVENLY -> IntArray(childCount + 1) { 1 }
    else -> error("Expected a distribution that uses all available space, but was $this.")
}
