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
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

@Composable
internal fun HorizontalStack(
    size: Size,
    dimension: Dimension.Horizontal,
    spacing: Dp,
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
        val fillSpaceSpacer: @Composable (Float) -> Unit = @Composable { weight ->
            Spacer(modifier = Modifier.weight(weight))
        }
        val latestContent by rememberUpdatedState(content)
        val scope = remember(dimension.distribution, spacing, latestContent) {
            HorizontalStackScopeImpl(
                distribution = dimension.distribution,
                spacing = spacing,
                fillSpaceSpacer = fillSpaceSpacer,
                width = size.width,
            ).apply(latestContent)
        }

        val edgeSpacerIfNeeded = @Composable {
            if (scope.shouldApplyFillSpacers &&
                (
                    dimension.distribution == FlexDistribution.SPACE_AROUND ||
                        dimension.distribution == FlexDistribution.SPACE_EVENLY
                    )
            ) {
                fillSpaceSpacer(1f)
            }
        }

        edgeSpacerIfNeeded()
        scope.content.invoke(this)
        edgeSpacerIfNeeded()
    }
}

internal interface HorizontalStackScope {
    fun items(
        children: List<ComponentStyle>,
        itemContent: @Composable RowScope.(index: Int, child: ComponentStyle) -> Unit,
    )
}

private class HorizontalStackScopeImpl(
    private val distribution: FlexDistribution,
    private val spacing: Dp,
    private val fillSpaceSpacer: @Composable (Float) -> Unit,
    private val width: SizeConstraint,
) : HorizontalStackScope {
    var content: @Composable RowScope.() -> Unit = {}
    private var hasAnyChildrenWithFillWidth = false
    val shouldApplyFillSpacers: Boolean
        get() = width != Fit && !hasAnyChildrenWithFillWidth

    override fun items(
        children: List<ComponentStyle>,
        itemContent: @Composable RowScope.(index: Int, child: ComponentStyle) -> Unit,
    ) {
        hasAnyChildrenWithFillWidth = children.any { it.size.width == Fill }
        content = {
            children.forEachIndexed { index, child ->
                val isLast = index == children.size - 1
                itemContent(index, child)

                if (distribution.usesAllAvailableSpace && !isLast) {
                    Spacer(modifier = Modifier.widthIn(min = spacing))
                    if (shouldApplyFillSpacers) {
                        fillSpaceSpacer(if (distribution == FlexDistribution.SPACE_AROUND) 2f else 1f)
                    }
                }
            }
        }
    }
}
