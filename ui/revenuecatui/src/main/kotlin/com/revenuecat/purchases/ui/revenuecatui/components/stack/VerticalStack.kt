@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A vertical stack of components which properly handles the arrangement of items.
 *
 * @param mainAxisUnbounded Whether this stack is being measured with an unbounded height (e.g. inside a vertical
 * scroll). Fill children are not given `weight` in that case, as they would collapse to zero.
 * @param itemContent Emits a child. The provided modifier must be applied to the child's outermost layout node.
 */
@Suppress("LongParameterList")
@Composable
internal fun VerticalStack(
    size: Size,
    dimension: Dimension.Vertical,
    spacing: Dp,
    items: List<ComponentStyle>,
    mainAxisUnbounded: Boolean,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    val hasAnyItemsWithFillHeight = items.any { it.size.height is Fill }
    if (needsConstrainedFillLayout(size, dimension.distribution, items, Orientation.Vertical)) {
        ConstrainedFillLayout(
            config = ConstrainedFillLayout.Config.Vertical(
                distribution = dimension.distribution,
                arrangement = dimension.distribution.toVerticalArrangement(spacing),
                alignment = dimension.alignment.toAlignment(),
                fitMainAxis = size.height.shouldFitMainAxis(hasAnyItemsWithFillHeight),
            ),
            spacing = spacing,
            modifier = modifier,
        ) {
            items.forEachIndexed { index, item ->
                itemContent(index, item, ComponentSizeParentDataModifier(item.size))
            }
        }
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = dimension.distribution.toVerticalArrangement(
            spacing = spacing,
        ),
        horizontalAlignment = dimension.alignment.toAlignment(),
    ) {
        val shouldApplyFillSpacers = size.height !is Fit && !hasAnyItemsWithFillHeight
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
            val childModifier = if (item.size.height is Fill && !mainAxisUnbounded) {
                Modifier.weight(1f)
            } else {
                Modifier
            }
            itemContent(index, item, childModifier)

            if (dimension.distribution.usesAllAvailableSpace && index != items.lastIndex) {
                Spacer(modifier = Modifier.heightIn(min = spacing))
                if (shouldApplyFillSpacers) {
                    fillSpaceSpacer(if (dimension.distribution == FlexDistribution.SPACE_AROUND) 2f else 1f)
                }
            }
        }
        edgeSpacerIfNeeded()
    }
}
