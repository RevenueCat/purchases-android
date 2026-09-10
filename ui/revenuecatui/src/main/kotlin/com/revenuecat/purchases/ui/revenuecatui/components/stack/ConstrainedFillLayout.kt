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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

/**
 * A Row/Column replacement that is only used when a stack has min/max size constraints on its main axis (see
 * [needsConstrainedFillLayout]). It divides the main axis between Fill children while honoring their minimums and
 * maximums, and lets a Fit stack with a positive minimum hug its content.
 */
internal object ConstrainedFillLayout {
    internal sealed interface Config {
        val orientation: Orientation
        val distribution: FlexDistribution
        val fitMainAxis: Boolean

        data class Horizontal(
            override val distribution: FlexDistribution,
            val arrangement: Arrangement.Horizontal,
            val alignment: Alignment.Vertical,
            override val fitMainAxis: Boolean = false,
        ) : Config {
            override val orientation: Orientation = Orientation.Horizontal
        }

        data class Vertical(
            override val distribution: FlexDistribution,
            val arrangement: Arrangement.Vertical,
            val alignment: Alignment.Horizontal,
            override val fitMainAxis: Boolean = false,
        ) : Config {
            override val orientation: Orientation = Orientation.Vertical
        }
    }

    /**
     * Every child is expected to carry a [ComponentSizeParentDataModifier] describing its resolved size. Children are
     * matched to their constraints through that parent data rather than by index, so children that are not composed
     * (e.g. `visible = false`) do not shift the constraints of their siblings.
     *
     * @param hasCrossAxisFillChild Whether any child is Fill on the cross axis. Under an unbounded cross axis (e.g. a
     * Fit row inside the root vertical scroll) such a child has nothing to fill and would collapse to its minimum.
     * Flexbox stretches it to the tallest sibling instead, so the layout is then bounded to its own intrinsic
     * cross-axis size, which Fill children can fill.
     */
    @Composable
    operator fun invoke(
        config: Config,
        spacing: Dp,
        hasCrossAxisFillChild: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Layout(
            modifier = modifier.conditional(hasCrossAxisFillChild) {
                boundUnboundedCrossAxisToIntrinsicSize(config.orientation)
            },
            content = content,
        ) { measurables, constraints ->
            measure(measurables, constraints, config, spacingPx = spacing.roundToPx())
        }
    }

    private fun Modifier.boundUnboundedCrossAxisToIntrinsicSize(orientation: Orientation): Modifier =
        layout { measurable, constraints ->
            val bounded = when {
                orientation == Orientation.Horizontal && constraints.maxHeight == Constraints.Infinity ->
                    constraints.copy(
                        maxHeight = measurable.maxIntrinsicHeight(constraints.maxWidth)
                            .coerceAtLeast(constraints.minHeight),
                    )
                orientation == Orientation.Vertical && constraints.maxWidth == Constraints.Infinity ->
                    constraints.copy(
                        maxWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
                            .coerceAtLeast(constraints.minWidth),
                    )
                else -> constraints
            }
            val placeable = measurable.measure(bounded)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
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

        val placeables: List<Placeable>
        val mainAxisSize: Int
        if (constraints.isFullyUnbounded(orientation)) {
            // Nothing to distribute: every child, Fill or not, wraps its own content.
            placeables = measurables.map { it.measure(constraints.forNonFillChild(consumed = 0, orientation)) }
            mainAxisSize = placeables.sumOf { it.mainAxisSize(orientation) } + totalSpacing
        } else {
            val measured = arrayOfNulls<Placeable>(measurables.size)
            measureNonFillChildren(measurables, fillConstraints, measured, constraints, config, spacingPx)
            val nonFillSize = measured.filterNotNull().sumOf { it.mainAxisSize(orientation) }
            mainAxisSize = targetMainAxisSize(config, constraints, fillConstraints, nonFillSize + totalSpacing)
            val availableForFill = (mainAxisSize - nonFillSize - totalSpacing).coerceAtLeast(0)
            val fillSizes = allocateConstrainedFillSpace(availableForFill, fillConstraints, density = this)
            measurables.forEachIndexed { index, measurable ->
                if (fillConstraints[index] != null) {
                    measured[index] = measurable.measure(
                        constraints.withExactMainAxisSize(fillSizes[index], orientation),
                    )
                }
            }
            placeables = measured.requireNoNulls().asList()
        }

        return layoutAndPlace(
            placeables = placeables,
            mainAxisSize = mainAxisSize,
            constraints = constraints,
            config = config,
            spacing = spacingPx,
        )
    }

    /**
     * A Fit stack with a positive minimum only grows to hug its content (with Fill children at their minimums); any
     * other stack takes all the space it is offered.
     */
    private fun MeasureScope.targetMainAxisSize(
        config: Config,
        constraints: Constraints,
        fillConstraints: List<Fill?>,
        contentSize: Int,
    ): Int {
        val orientation = config.orientation
        if (!config.fitMainAxis) {
            return constraints.mainAxisMax(orientation)
                .takeUnless { it == Constraints.Infinity }
                ?: constraints.mainAxisMin(orientation)
        }
        val minimumFillSize = allocateConstrainedFillSpace(availableSpace = 0, fillConstraints, density = this).sum()
        return (contentSize + minimumFillSize).coerceIn(
            constraints.mainAxisMin(orientation),
            constraints.mainAxisMax(orientation),
        )
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

private fun Constraints.isFullyUnbounded(orientation: Orientation): Boolean =
    mainAxisMax(orientation) == Constraints.Infinity && mainAxisMin(orientation) == 0

private fun Constraints.mainAxisMin(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) minWidth else minHeight

private fun Constraints.mainAxisMax(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) maxWidth else maxHeight

private fun Constraints.crossAxisMin(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) minHeight else minWidth

private fun Constraints.crossAxisMax(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) maxHeight else maxWidth

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

private fun Constraints.withExactMainAxisSize(size: Int, orientation: Orientation): Constraints =
    if (orientation == Orientation.Horizontal) {
        copy(minWidth = size, maxWidth = size, minHeight = 0)
    } else {
        copy(minWidth = 0, minHeight = size, maxHeight = size)
    }

private fun Placeable.mainAxisSize(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) width else height

private fun Placeable.crossAxisSize(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) height else width

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
    val resolvedCrossAxisSize = placeables.maxOfOrNull { it.crossAxisSize(orientation) }
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
