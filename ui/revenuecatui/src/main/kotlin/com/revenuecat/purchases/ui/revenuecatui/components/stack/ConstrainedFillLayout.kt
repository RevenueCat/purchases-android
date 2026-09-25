@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier

internal object ConstrainedFillLayout {
    internal sealed interface Config {
        val orientation: Orientation
        val distribution: FlexDistribution

        data class Horizontal(
            override val distribution: FlexDistribution,
            val arrangement: Arrangement.Horizontal,
            val alignment: Alignment.Vertical,
        ) : Config {
            override val orientation: Orientation = Orientation.Horizontal
        }

        data class Vertical(
            override val distribution: FlexDistribution,
            val arrangement: Arrangement.Vertical,
            val alignment: Alignment.Horizontal,
        ) : Config {
            override val orientation: Orientation = Orientation.Vertical
        }
    }

    /**
     * Every child is expected to carry a [ComponentSizeParentDataModifier] describing its resolved size. Children are
     * matched to their constraints through that parent data rather than by index, so children that are not composed
     * (e.g. `visible = false`) do not shift the constraints of their siblings.
     */
    @Composable
    operator fun invoke(
        config: Config,
        spacing: Dp,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Layout(modifier = modifier, content = content) { measurables, constraints ->
            measure(measurables, constraints, config, spacingPx = spacing.roundToPx())
        }
    }

    private fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
        config: Config,
        spacingPx: Int,
    ): MeasureResult {
        val orientation = config.orientation
        val fillConstraints = measurables.map {
            (it.parentData as? ComponentSizeParentDataModifier)?.size?.mainAxis(orientation) as? Fill
        }
        val totalSpacing = spacingPx * (measurables.size - 1).coerceAtLeast(0)

        if (constraints.isFullyUnbounded(orientation)) {
            // Nothing to distribute: every child, Fill or not, wraps its own content.
            val measured = measurables.map { it.measure(constraints.forNonFillChild(consumed = 0, orientation)) }
            return layoutAndPlace(
                placeables = measured,
                mainAxisSize = measured.sumOf { it.mainAxisSize(orientation) } + totalSpacing,
                constraints = constraints,
                config = config,
                spacing = spacingPx,
            )
        }

        val placeables = arrayOfNulls<Placeable>(measurables.size)
        measureNonFillChildren(measurables, fillConstraints, placeables, constraints, config, spacingPx)
        val nonFillSize = placeables.filterNotNull().sumOf { it.mainAxisSize(orientation) }

        // Routing considers every possible override, but parent data describes only the children and sizes currently
        // composed. If no Fill child remains (e.g. the limited-Fill candidate is hidden), preserve Row/Column's wrap
        // behavior instead of expanding a Fit stack to the parent's maximum.
        return if (fillConstraints.all { it == null }) {
            layoutAndPlace(
                placeables = placeables.requireNoNulls().asList(),
                mainAxisSize = nonFillSize + totalSpacing,
                constraints = constraints,
                config = config,
                spacing = spacingPx,
            )
        } else {
            val targetMainAxisSize = constraints.targetMainAxisSize(orientation)
            val availableForFill = (targetMainAxisSize - nonFillSize - totalSpacing).coerceAtLeast(0)
            val fillSizes = allocateConstrainedFillSpace(availableForFill, fillConstraints, this)
            measurables.forEachIndexed { index, measurable ->
                if (fillConstraints[index] != null) {
                    placeables[index] = measurable.measure(
                        constraints.withExactMainAxisSize(fillSizes[index], orientation),
                    )
                }
            }

            // Inside a scroll the max is infinite and the target falls back to the viewport (the min). Fill children
            // still share that viewport, but the stack itself has to report its full content size or the scroll
            // has no range and everything past the viewport is unreachable.
            val measured = placeables.requireNoNulls().asList()
            val contentSize = measured.sumOf { it.mainAxisSize(orientation) } + totalSpacing
            layoutAndPlace(
                placeables = measured,
                mainAxisSize = maxOf(targetMainAxisSize, contentSize),
                constraints = constraints,
                config = config,
                spacing = spacingPx,
            )
        }
    }

    /**
     * Measures every non-Fill child into [placeables], mirroring Row/Column: spacedBy distributions reserve spacing
     * after each measured non-Fill child, while SPACE_* distributions use explicit spacers, which reserve spacing
     * after *every* preceding child.
     */
    @Suppress("LongParameterList")
    private fun measureNonFillChildren(
        measurables: List<Measurable>,
        fillConstraints: List<Fill?>,
        placeables: Array<Placeable?>,
        constraints: Constraints,
        config: Config,
        spacingPx: Int,
    ) {
        var nonFillSize = 0
        var nonFillCount = 0
        measurables.forEachIndexed { index, measurable ->
            if (fillConstraints[index] != null) return@forEachIndexed
            val gaps = if (config.distribution.usesAllAvailableSpace) index else nonFillCount
            val consumed = nonFillSize + spacingPx * gaps
            val placeable = measurable.measure(constraints.forNonFillChild(consumed, config.orientation))
            placeables[index] = placeable
            nonFillSize += placeable.mainAxisSize(config.orientation)
            nonFillCount++
        }
    }
}

/**
 * Like Row/Column, non-Fill children are measured without minimums and are only offered the main-axis space that
 * previous siblings left over.
 */
private fun Constraints.forNonFillChild(consumed: Int, orientation: Orientation): Constraints {
    val mainAxisMax = mainAxisMax(orientation)
    if (mainAxisMax == Constraints.Infinity) return copy(minWidth = 0, minHeight = 0)
    val remaining = (mainAxisMax - consumed).coerceAtLeast(0)
    return if (orientation == Orientation.Horizontal) {
        copy(minWidth = 0, minHeight = 0, maxWidth = remaining)
    } else {
        copy(minWidth = 0, minHeight = 0, maxHeight = remaining)
    }
}

private fun Constraints.isFullyUnbounded(orientation: Orientation): Boolean =
    mainAxisMax(orientation) == Constraints.Infinity && mainAxisMin(orientation) == 0

private fun Constraints.targetMainAxisSize(orientation: Orientation): Int =
    mainAxisMax(orientation).takeUnless { it == Constraints.Infinity } ?: mainAxisMin(orientation)

private fun Constraints.mainAxisMin(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) minWidth else minHeight

private fun Constraints.mainAxisMax(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) maxWidth else maxHeight

private fun Constraints.withExactMainAxisSize(size: Int, orientation: Orientation): Constraints =
    if (orientation == Orientation.Horizontal) {
        copy(minWidth = size, maxWidth = size, minHeight = 0)
    } else {
        copy(minWidth = 0, minHeight = size, maxHeight = size)
    }

private fun Placeable.mainAxisSize(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) width else height

private fun MeasureScope.layoutAndPlace(
    placeables: List<Placeable>,
    mainAxisSize: Int,
    constraints: Constraints,
    config: ConstrainedFillLayout.Config,
    spacing: Int,
): MeasureResult {
    val orientation = config.orientation
    val resolvedMainAxisSize = mainAxisSize.coerceIn(
        constraints.mainAxisMin(orientation),
        constraints.mainAxisMax(orientation),
    )
    val resolvedCrossAxisSize = placeables.maxOfOrNull {
        if (orientation == Orientation.Horizontal) it.height else it.width
    }
        ?.coerceIn(constraints.crossAxisMin(orientation), constraints.crossAxisMax(orientation))
        ?: constraints.crossAxisMin(orientation)
    val sizes = placeables.map { it.mainAxisSize(orientation) }.toIntArray()
    val positions = arrangeConstrainedFillItems(
        config = config,
        totalSize = resolvedMainAxisSize,
        sizes = sizes,
        spacing = spacing,
    )

    val width = if (orientation == Orientation.Horizontal) resolvedMainAxisSize else resolvedCrossAxisSize
    val height = if (orientation == Orientation.Horizontal) resolvedCrossAxisSize else resolvedMainAxisSize
    return layout(width, height) {
        placeables.forEachIndexed { index, placeable ->
            val crossAxisPosition = when (config) {
                is ConstrainedFillLayout.Config.Horizontal ->
                    config.alignment.align(placeable.height, resolvedCrossAxisSize)
                is ConstrainedFillLayout.Config.Vertical ->
                    config.alignment.align(placeable.width, resolvedCrossAxisSize, layoutDirection)
            }
            val x = if (orientation == Orientation.Horizontal) positions[index] else crossAxisPosition
            val y = if (orientation == Orientation.Horizontal) crossAxisPosition else positions[index]
            placeable.place(x, y)
        }
    }
}

private fun Constraints.crossAxisMin(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) minHeight else minWidth

private fun Constraints.crossAxisMax(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) maxHeight else maxWidth
