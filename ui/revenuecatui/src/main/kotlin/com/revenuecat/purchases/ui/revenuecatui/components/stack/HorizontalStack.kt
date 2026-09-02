@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
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
    if (!mainAxisUnbounded && items.hasConstrainedFillWidth) {
        ConstrainedFillRow(
            items = items,
            dimension = dimension,
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
        val hasAnyFillWidth = items.any { it.size.width is Fill }
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
            val fillWidth = item.size.width as? Fill
            val itemModifier = if (fillWidth != null && !mainAxisUnbounded) {
                Modifier.weight(1f, fill = fillWidth.max == null)
            } else {
                Modifier
            }
            itemContent(index, item, itemModifier)

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

private val List<ComponentStyle>.hasConstrainedFillWidth: Boolean
    get() = any {
        val fill = it.size.width as? Fill
        fill != null && (fill.min != null || fill.max != null)
    }

@Composable
private fun ConstrainedFillRow(
    items: List<ComponentStyle>,
    dimension: Dimension.Horizontal,
    spacing: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    val arrangement: Arrangement.Horizontal = if (dimension.distribution.usesAllAvailableSpace) {
        Arrangement.spacedBy(spacing, Alignment.Start)
    } else {
        dimension.distribution.toHorizontalArrangement(spacing)
    }
    ConstrainedFillLayout(
        config = ConstrainedFillLayout.Config.Horizontal(
            arrangement = arrangement,
            alignment = dimension.alignment.toAlignment(),
        ),
        fillConstraints = items.map { it.size.width as? Fill },
        spacing = spacing,
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            itemContent(index, item, Modifier)
        }
    }
}
