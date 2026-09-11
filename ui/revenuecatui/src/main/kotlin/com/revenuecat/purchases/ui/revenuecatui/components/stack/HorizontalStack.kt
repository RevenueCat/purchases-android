@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A horizontal stack of components which properly handles the arrangement of items.
 *
 * @param mainAxisUnbounded Whether this stack is being measured with an unbounded width (e.g. inside a horizontal
 * scroll). Fill children are not given `weight` in that case, as they would collapse to zero.
 * @param itemContent Emits a child. The provided modifier must be applied to the child's outermost layout node.
 */
@Suppress("LongParameterList")
@Composable
internal fun HorizontalStack(
    size: Size,
    dimension: Dimension.Horizontal,
    spacing: Dp,
    items: List<ComponentStyle>,
    mainAxisUnbounded: Boolean,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    val hasAnyItemsWithFillWidth = items.any { it.size.width is Fill }
    if (needsConstrainedFillLayout(size, dimension.distribution, items, Orientation.Horizontal)) {
        ConstrainedFillLayout(
            config = ConstrainedFillLayout.Config.Horizontal(
                distribution = dimension.distribution,
                arrangement = dimension.distribution.toHorizontalArrangement(spacing),
                alignment = dimension.alignment.toAlignment(),
                fitMainAxis = size.width.shouldFitMainAxis(hasAnyItemsWithFillWidth),
            ),
            spacing = spacing,
            hasCrossAxisFillChild = items.any { it.size.height is Fill },
            modifier = modifier,
        ) {
            items.forEachIndexed { index, item ->
                itemContent(index, item, ComponentSizeParentDataModifier(item.size))
            }
        }
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = dimension.alignment.toAlignment(),
        horizontalArrangement = dimension.distribution.toHorizontalArrangement(
            spacing = spacing,
        ),
    ) {
        val shouldApplyFillSpacers = size.width !is Fit && !hasAnyItemsWithFillWidth
        val fillSpaceSpacer: @Composable (Float) -> Unit = @Composable { weight ->
            Spacer(modifier = Modifier.weight(weight))
        }

        val edgeSpacerIfNeeded = @Composable {
            if (shouldApplyFillSpacers &&
                (
                    dimension.distribution == FlexDistribution.SPACE_AROUND ||
                        dimension.distribution == FlexDistribution.SPACE_EVENLY
                    )
            ) {
                fillSpaceSpacer(1f)
            }
        }

        edgeSpacerIfNeeded()
        items.forEachIndexed { index, item ->
            val childModifier = if (item.size.width is Fill && !mainAxisUnbounded) {
                Modifier.weight(1f)
            } else {
                Modifier
            }
            itemContent(index, item, childModifier)

            if (dimension.distribution.usesAllAvailableSpace && index != items.lastIndex) {
                Spacer(modifier = Modifier.widthIn(min = spacing))
                if (shouldApplyFillSpacers) {
                    fillSpaceSpacer(if (dimension.distribution == FlexDistribution.SPACE_AROUND) 2f else 1f)
                }
            }
        }
        edgeSpacerIfNeeded()
    }
}
