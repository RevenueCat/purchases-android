@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A vertical stack of components which properly handles the arrangement of items.
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
    if (!mainAxisUnbounded && items.hasConstrainedFillHeight) {
        ConstrainedFillColumn(
            items = items,
            dimension = dimension,
            spacing = spacing,
            modifier = modifier,
            itemContent = itemContent,
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = dimension.distribution.toVerticalArrangement(
            spacing = spacing,
        ),
        horizontalAlignment = dimension.alignment.toAlignment(),
    ) {
        val hasAnyFillHeight = items.any { it.size.height is Fill }
        val shouldApplyFillSpacers = size.height.allowsFlexDistribution && !hasAnyFillHeight
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
            val fillHeight = item.size.height as? Fill
            val itemModifier = if (fillHeight != null && !mainAxisUnbounded) {
                Modifier.weight(1f, fill = fillHeight.max == null)
            } else {
                Modifier
            }
            itemContent(index, item, itemModifier)

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

private val List<ComponentStyle>.hasConstrainedFillHeight: Boolean
    get() = any {
        val fill = it.size.height as? Fill
        fill != null && (fill.min != null || fill.max != null)
    }

@Composable
private fun ConstrainedFillColumn(
    items: List<ComponentStyle>,
    dimension: Dimension.Vertical,
    spacing: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    ConstrainedFillLayout(
        config = ConstrainedFillLayout.Config.Vertical(
            distribution = dimension.distribution,
            arrangement = dimension.distribution.toVerticalArrangement(spacing),
            alignment = dimension.alignment.toAlignment(),
        ),
        fillConstraints = items.map { it.size.height as? Fill },
        spacing = spacing,
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            itemContent(index, item, Modifier)
        }
    }
}
