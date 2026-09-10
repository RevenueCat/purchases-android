@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier

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
     * Every child is expected to carry a [ComponentSizeParentDataModifier] describing its resolved size.
     * Children are matched to their constraints through that parent data rather than by index, so children that are
     * not composed (e.g. `visible = false`) do not shift the constraints of their siblings.
     */
    @Composable
    @Suppress("DEPRECATION")
    operator fun invoke(
        config: Config,
        spacing: Dp,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        MultiMeasureLayout(modifier = modifier, content = content) { measurables, constraints ->
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
        val resolvedSizes = measurables.map { (it.parentData as? ComponentSizeParentDataModifier)?.size }
        val fillConstraints = resolvedSizes.map { it?.mainAxisConstraint(orientation) as? Fill }
        val totalSpacing = spacingPx * (measurables.size - 1).coerceAtLeast(0)

        val placeables: Array<Placeable>
        val mainAxisSize: Int
        if (constraints.isFullyUnbounded(orientation)) {
            placeables = Array(measurables.size) { index ->
                measurables[index].measure(constraints.forNonFillChild(consumed = 0, orientation))
            }
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
            placeables = measured.requireNoNulls()
        }

        remeasureCrossAxisFillChildren(
            measurables = measurables,
            resolvedSizes = resolvedSizes,
            placeables = placeables,
            constraints = constraints,
            orientation = orientation,
        )
        return layoutAndPlace(
            placeables = placeables.asList(),
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

    /**
     * Under an unbounded cross axis (e.g. a Fit row inside the root vertical scroll) a cross-axis Fill child has
     * nothing to fill and collapses to its minimum. Flexbox instead stretches it to the tallest sibling, so derive
     * the stack's cross-axis size from the non-Fill siblings and remeasure the Fill children to exactly that.
     *
     * A bounded cross axis is deliberately left alone: there the Fill child already resolved against the parent's
     * limit in `Modifier.size`, which is the behavior existing paywalls were built against.
     */
    private fun MeasureScope.remeasureCrossAxisFillChildren(
        measurables: List<Measurable>,
        resolvedSizes: List<Size?>,
        placeables: Array<Placeable>,
        constraints: Constraints,
        orientation: Orientation,
    ) {
        if (constraints.crossAxisMax(orientation) != Constraints.Infinity) return

        val crossAxisFills = resolvedSizes.map { it?.crossAxisConstraint(orientation) as? Fill }
        if (crossAxisFills.all { it == null }) return

        val nonFillCrossAxisSize = placeables.indices
            .filter { crossAxisFills[it] == null }
            .maxOfOrNull { placeables[it].crossAxisSize(orientation) }
            ?: 0
        val fillMinimum = crossAxisFills.maxOf { it?.min?.toPx(density = this) ?: 0 }
        val targetCrossAxisSize = maxOf(constraints.crossAxisMin(orientation), nonFillCrossAxisSize, fillMinimum)

        crossAxisFills.forEachIndexed { index, fill ->
            if (fill == null) return@forEachIndexed
            placeables[index] = measurables[index].measure(
                Constraints.exact(
                    mainAxisSize = placeables[index].mainAxisSize(orientation),
                    crossAxisSize = targetCrossAxisSize,
                    orientation = orientation,
                ),
            )
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

private fun Constraints.withExactMainAxisSize(size: Int, orientation: Orientation): Constraints =
    if (orientation == Orientation.Horizontal) {
        copy(minWidth = size, maxWidth = size, minHeight = 0)
    } else {
        copy(minWidth = 0, minHeight = size, maxHeight = size)
    }

private fun Constraints.Companion.exact(
    mainAxisSize: Int,
    crossAxisSize: Int,
    orientation: Orientation,
): Constraints = if (orientation == Orientation.Horizontal) {
    fixed(width = mainAxisSize, height = crossAxisSize)
} else {
    fixed(width = crossAxisSize, height = mainAxisSize)
}

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
