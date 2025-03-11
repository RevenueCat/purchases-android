package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement

@Composable
internal fun HorizontalStack(
    size: Size,
    dimension: Dimension.Horizontal,
    spacing: Dp,
    hasAnyChildrenWithFillWidth: Boolean,
    content: HorizontalStackScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = dimension.alignment.toAlignment(),
        horizontalArrangement = dimension.distribution.toHorizontalArrangement(
            spacing = spacing,
        ),
    ) {
        val shouldApplyFillSpacers = size.width != Fit && !hasAnyChildrenWithFillWidth
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

        val latestContent by rememberUpdatedState(content)
        val scope = remember(dimension.distribution, spacing) {
            HorizontalStackScopeImpl(
                distribution = dimension.distribution,
                spacing = spacing,
                fillSpaceSpacer = if (shouldApplyFillSpacers) fillSpaceSpacer else null,
            )
        }

        edgeSpacerIfNeeded()

        scope.latestContent()
        scope.content.invoke(this)

        edgeSpacerIfNeeded()
    }
}

internal interface HorizontalStackScope {
    fun items(
        count: Int,
        itemContent: @Composable RowScope.(index: Int) -> Unit,
    )
}

private class HorizontalStackScopeImpl(
    private val distribution: FlexDistribution,
    private val spacing: Dp,
    private val fillSpaceSpacer: (@Composable (Float) -> Unit)?,
) : HorizontalStackScope {
    var content: @Composable RowScope.() -> Unit = {}

    override fun items(count: Int, itemContent: @Composable RowScope.(index: Int) -> Unit) {
        content = {
            repeat(count) { index ->
                val isLast = index == count - 1
                itemContent(index)

                if (distribution.usesAllAvailableSpace && !isLast) {
                    Spacer(modifier = Modifier.widthIn(min = spacing))
                    fillSpaceSpacer?.invoke(if (distribution == FlexDistribution.SPACE_AROUND) 2f else 1f)
                }
            }
        }
    }
}
