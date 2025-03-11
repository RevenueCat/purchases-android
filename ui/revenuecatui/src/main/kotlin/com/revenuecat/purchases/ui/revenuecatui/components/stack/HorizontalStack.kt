package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@Composable
internal fun HorizontalStack(
    size: Size,
    dimension: Dimension.Horizontal,
    spacing: Dp,
    topSystemBarsPadding: PaddingValues,
    children: List<ComponentStyle>,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    contentAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = dimension.alignment.toAlignment(),
        horizontalArrangement = dimension.distribution.toHorizontalArrangement(
            spacing = spacing,
        ),
    ) {
        val hasChildrenWithFillWidth = children.any { it.size.width == Fill }
        val shouldApplyFillSpacers = size.width != Fit && !hasChildrenWithFillWidth
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

        children.forEachIndexed { index, child ->
            val isLast = index == children.size - 1
            val childPadding = if (child.ignoreTopWindowInsets) {
                PaddingValues(all = 0.dp)
            } else {
                topSystemBarsPadding
            }

            ComponentView(
                style = child,
                state = state,
                onClick = clickHandler,
                modifier = Modifier
                    .conditional(child.size.width == Fill) { Modifier.weight(1f) }
                    .padding(childPadding)
                    .alpha(contentAlpha),
            )

            if (dimension.distribution.usesAllAvailableSpace && !isLast) {
                Spacer(modifier = Modifier.widthIn(min = spacing))
                if (shouldApplyFillSpacers) {
                    fillSpaceSpacer(if (dimension.distribution == FlexDistribution.SPACE_AROUND) 2f else 1f)
                }
            }
        }

        edgeSpacerIfNeeded()
    }
}
