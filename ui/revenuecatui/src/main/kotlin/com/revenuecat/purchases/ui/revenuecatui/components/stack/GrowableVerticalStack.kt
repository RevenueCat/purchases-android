@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A vertical stack used when a child is on a fill-height web view chain (see
 * [growsToWebViewContentHeight]). [VerticalStack] sizes fill-height children with
 * `Modifier.weight`, which measures them at an exact share of the available space — a size they can
 * never exceed, because Compose clamps a child's reported size to the constraints it was measured
 * under. Web content taller than that share then has nowhere to go and the outer scroll never
 * engages. This layout hands each fill-height child its share as a *minimum* instead:
 *
 * - With a bounded height, children are measured with `min = max = share` — identical to weight.
 * - With an unbounded maximum (an ancestor scrolls), children are measured with
 *   `min = share, max = ∞`: the share is the space weight would have given them, computed against
 *   the minimum height this stack was asked to keep (the viewport, under the root paywall scroll).
 *   A child whose content fits its share stretches to it (fill), and one whose content is taller
 *   grows past it, pushing this stack — and the scroll around it — beyond the viewport.
 *
 * The floor survives nesting without any signaling: a fill-height child stack's `fillMaxHeight`
 * passes an unbounded constraint through unchanged, and if that child is itself on the chain it
 * uses this layout too, subdividing the floor it received among its own children.
 *
 * [itemContent] must apply the provided item modifier to the item's single root layout node: it
 * carries the [layoutId] this layout uses to map measurables back to [children], which keeps the
 * mapping correct when some children compose to nothing (e.g. not visible).
 */
@Composable
internal fun GrowableVerticalStack(
    children: List<ComponentStyle>,
    dimension: Dimension.Vertical,
    spacing: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, itemModifier: Modifier) -> Unit,
) {
    val alignment = dimension.alignment.toAlignment()
    Layout(
        content = {
            children.forEachIndexed { index, child ->
                itemContent(index, child, Modifier.layoutId(index))
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        measureGrowableColumn(
            measurables = measurables,
            constraints = constraints,
            isFillHeight = measurables.map { measurable ->
                (measurable.layoutId as? Int)
                    ?.let { children.getOrNull(it)?.size?.height == Fill }
                    ?: false
            },
            spacingPx = spacing.roundToPx(),
            distribution = dimension.distribution,
            alignment = alignment,
        )
    }
}

@Suppress("LongParameterList")
private fun MeasureScope.measureGrowableColumn(
    measurables: List<Measurable>,
    constraints: Constraints,
    isFillHeight: List<Boolean>,
    spacingPx: Int,
    distribution: FlexDistribution,
    alignment: Alignment.Horizontal,
): MeasureResult {
    val totalSpacing = spacingPx * (measurables.size - 1).coerceAtLeast(0)
    val placeables = arrayOfNulls<Placeable>(measurables.size)

    // Fit/fixed children first: their natural heights determine the space left for fill children.
    var nonFillHeight = 0
    measurables.forEachIndexed { index, measurable ->
        if (!isFillHeight[index]) {
            val placeable = measurable.measure(
                Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight),
            )
            placeables[index] = placeable
            nonFillHeight += placeable.height
        }
    }

    // Each fill child's share of the space this stack has to hand out: the full bounded height, or
    // the minimum it was asked to keep when the maximum is unbounded.
    val fillCount = isFillHeight.count { it }
    val budget = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
    val available = (budget - nonFillHeight - totalSpacing).coerceAtLeast(0)
    val baseShare = if (fillCount > 0) available / fillCount else 0
    var shareRemainder = if (fillCount > 0) available % fillCount else 0

    var fillHeight = 0
    measurables.forEachIndexed { index, measurable ->
        if (isFillHeight[index]) {
            val share = baseShare + if (shareRemainder-- > 0) 1 else 0
            val placeable = measurable.measure(
                if (constraints.hasBoundedHeight) {
                    Constraints(maxWidth = constraints.maxWidth, minHeight = share, maxHeight = share)
                } else {
                    // The share is a floor, not a ceiling: content that reports its own height (a
                    // web view) may measure taller and push this stack past its minimum.
                    Constraints(maxWidth = constraints.maxWidth, minHeight = share)
                },
            )
            placeables[index] = placeable
            fillHeight += placeable.height
        }
    }

    val contentHeight = nonFillHeight + fillHeight + totalSpacing
    val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
    val width = (placeables.maxOfOrNull { it?.width ?: 0 } ?: 0)
        .coerceIn(constraints.minWidth, constraints.maxWidth)

    // Leftover is zero whenever a fill child is present (the shares consume it by construction);
    // the distribution only decides placement in the degenerate cases (e.g. every fill child
    // composed to nothing).
    val leftover = (height - contentHeight).coerceAtLeast(0)
    val gapCount = (measurables.size - 1).coerceAtLeast(0)
    val (leadingSpace, extraGap) = distribution.distributeLeftover(leftover, gapCount)

    return layout(width, height) {
        var y = leadingSpace
        placeables.forEach { placeable ->
            if (placeable == null) return@forEach
            val x = alignment.align(placeable.width, width, layoutDirection)
            placeable.place(x, y)
            y += placeable.height + spacingPx + extraGap
        }
    }
}

/** Splits [leftover] main-axis space into (space before the first child, extra space per gap). */
private fun FlexDistribution.distributeLeftover(leftover: Int, gapCount: Int): Pair<Int, Int> =
    when (this) {
        FlexDistribution.START -> 0 to 0
        FlexDistribution.CENTER -> leftover / 2 to 0
        FlexDistribution.END -> leftover to 0
        FlexDistribution.SPACE_BETWEEN ->
            if (gapCount > 0) 0 to leftover / gapCount else leftover / 2 to 0
        FlexDistribution.SPACE_AROUND -> {
            val gap = leftover / (gapCount + 1)
            gap / 2 to gap
        }
        FlexDistribution.SPACE_EVENLY -> {
            val gap = leftover / (gapCount + 2)
            gap to gap
        }
    }
