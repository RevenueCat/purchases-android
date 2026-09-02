@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill

internal object ConstrainedFillLayout {
    sealed interface Config {
        val orientation: Orientation

        class Horizontal(
            val arrangement: Arrangement.Horizontal,
            val alignment: Alignment.Vertical,
        ) : Config {
            override val orientation: Orientation = Orientation.Horizontal
        }

        class Vertical(
            val arrangement: Arrangement.Vertical,
            val alignment: Alignment.Horizontal,
        ) : Config {
            override val orientation: Orientation = Orientation.Vertical
        }
    }

    @Composable
    operator fun invoke(
        config: Config,
        fillConstraints: List<Fill?>,
        spacing: Dp,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Layout(modifier = modifier, content = content) { measurables, constraints ->
            val spacingPx = spacing.roundToPx()
            val totalSpacing = spacingPx * (measurables.size - 1).coerceAtLeast(0)

            if (constraints.isFullyUnbounded(config.orientation)) {
                val measured = measurables.map { it.measure(constraints.withZeroMinimums()) }
                return@Layout layoutAndPlace(
                    placeables = measured,
                    mainAxisSize = measured.sumOf { it.mainAxisSize(config.orientation) } + totalSpacing,
                    constraints = constraints,
                    config = config,
                )
            }

            val targetMainAxisSize = constraints.targetMainAxisSize(config.orientation)
            val placeables = arrayOfNulls<Placeable>(measurables.size)
            var nonFillSize = 0
            measurables.forEachIndexed { index, measurable ->
                if (fillConstraints[index] == null) {
                    placeables[index] = measurable.measure(constraints.withZeroMinimums())
                    nonFillSize += placeables[index]!!.mainAxisSize(config.orientation)
                }
            }

            val availableForFill = (targetMainAxisSize - nonFillSize - totalSpacing).coerceAtLeast(0)
            val fillSizes = allocateConstrainedFillSpace(availableForFill, fillConstraints, this)
            measurables.forEachIndexed { index, measurable ->
                if (fillConstraints[index] != null) {
                    placeables[index] = measurable.measure(
                        constraints.withExactMainAxisSize(fillSizes[index], config.orientation),
                    )
                }
            }

            layoutAndPlace(
                placeables = placeables.requireNoNulls().asList(),
                mainAxisSize = targetMainAxisSize,
                constraints = constraints,
                config = config,
            )
        }
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

private fun Constraints.withZeroMinimums(): Constraints = copy(minWidth = 0, minHeight = 0)

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
    val positions = IntArray(placeables.size)
    val sizes = placeables.map { it.mainAxisSize(orientation) }.toIntArray()
    when (config) {
        is ConstrainedFillLayout.Config.Horizontal -> with(config.arrangement) {
            arrange(resolvedMainAxisSize, sizes, layoutDirection, positions)
        }
        is ConstrainedFillLayout.Config.Vertical -> with(config.arrangement) {
            arrange(resolvedMainAxisSize, sizes, positions)
        }
    }

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
