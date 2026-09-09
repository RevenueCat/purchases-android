@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A horizontal stack of components which properly handles the arrangement of items.
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
    // Children resolve their size (including overrides and margins) themselves, so the stack cannot know up front
    // whether a Fill child is constrained. Any Fill child therefore goes through ConstrainedFillLayout, which reads
    // the resolved size from parent data at measure time.
    val hasAnyFillWidth = items.any { it.size.width is Fill }
    val fitMinimumUsesFlexDistribution = size.width.requiresFitMinimumLayout(dimension.distribution)
    if (!mainAxisUnbounded && (hasAnyFillWidth || fitMinimumUsesFlexDistribution)) {
        ConstrainedFillRow(
            items = items,
            config = ConstrainedFillLayout.Config.Horizontal(
                distribution = dimension.distribution,
                arrangement = dimension.distribution.toHorizontalArrangement(spacing),
                alignment = dimension.alignment.toAlignment(),
                fitMainAxis = size.width.hasPositiveFitMinimum,
            ),
            spacing = spacing,
            modifier = modifier,
            itemContent = itemContent,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = dimension.alignment.toAlignment(),
        horizontalArrangement = dimension.distribution.toHorizontalArrangement(
            spacing = spacing,
        ),
    ) {
        val shouldApplyFillSpacers = size.width.allowsFlexDistribution && !hasAnyFillWidth
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
            itemContent(index, item, Modifier)

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

@Composable
private fun ConstrainedFillRow(
    items: List<ComponentStyle>,
    config: ConstrainedFillLayout.Config.Horizontal,
    spacing: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    ConstrainedFillLayout(
        config = config,
        spacing = spacing,
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            itemContent(
                index,
                item,
                Modifier.then(ComponentSizeParentDataModifier(item.size)),
            )
        }
    }
}
