package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

// Pre-fix Row/Column layout, retained as a bounded-layout compatibility reference.

@Suppress("LongParameterList")
// Pre-fix Row/Column implementation, retained as a compatibility reference for bounded layouts.
@Composable
internal fun referenceHorizontalStack(
    size: Size,
    dimension: Dimension.Horizontal,
    spacing: Dp,
    items: List<ComponentStyle>,
    mainAxisUnbounded: Boolean,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = dimension.alignment.toAlignment(),
        horizontalArrangement = dimension.distribution.toHorizontalArrangement(
            spacing = spacing,
        ),
    ) {
        val hasAnyItemsWithFillWidth = items.any { it.size.width is Fill }
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


@Suppress("LongParameterList")
@Composable
internal fun referenceVerticalStack(
    size: Size,
    dimension: Dimension.Vertical,
    spacing: Dp,
    items: List<ComponentStyle>,
    mainAxisUnbounded: Boolean,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = dimension.distribution.toVerticalArrangement(
            spacing = spacing,
        ),
        horizontalAlignment = dimension.alignment.toAlignment(),
    ) {
        val hasAnyItemsWithFillHeight = items.any { it.size.height is Fill }
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
