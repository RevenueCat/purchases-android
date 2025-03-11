package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A vertical stack of components which properly handles the arrangement of items.
 */
@Composable
internal fun VerticalStack(
    size: Size,
    dimension: Dimension.Vertical,
    spacing: Dp,
    content: VerticalStackScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = dimension.distribution.toVerticalArrangement(
            spacing = spacing,
        ),
        horizontalAlignment = dimension.alignment.toAlignment(),
    ) {
        val fillSpaceSpacer: @Composable (Float) -> Unit = @Composable { weight ->
            Spacer(modifier = Modifier.weight(weight))
        }
        val latestContent by rememberUpdatedState(content)
        val scope = remember(dimension.distribution, spacing, latestContent) {
            VerticalStackScopeImpl(
                distribution = dimension.distribution,
                spacing = spacing,
                fillSpaceSpacer = fillSpaceSpacer,
                height = size.height,
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
        scope.columnContent(this)
        edgeSpacerIfNeeded()
    }
}

internal interface VerticalStackScope {
    fun items(
        items: List<ComponentStyle>,
        itemContent: @Composable ColumnScope.(index: Int, item: ComponentStyle) -> Unit,
    )
}

private class VerticalStackScopeImpl(
    private val distribution: FlexDistribution,
    private val spacing: Dp,
    private val fillSpaceSpacer: @Composable (Float) -> Unit,
    private val height: SizeConstraint,
) : VerticalStackScope {
    private var hasAnyItemsWithFillHeight = false
    val shouldApplyFillSpacers: Boolean
        get() = height != Fit && !hasAnyItemsWithFillHeight
    var columnContent: @Composable ColumnScope.() -> Unit = {}

    override fun items(
        items: List<ComponentStyle>,
        itemContent: @Composable ColumnScope.(index: Int, item: ComponentStyle) -> Unit,
    ) {
        hasAnyItemsWithFillHeight = items.any { it.size.height == Fill }
        columnContent = {
            items.forEachIndexed { index, item ->
                val isLast = index == items.size - 1
                itemContent(index, item)

                if (distribution.usesAllAvailableSpace && !isLast) {
                    Spacer(modifier = Modifier.heightIn(min = spacing))
                    if (shouldApplyFillSpacers) {
                        fillSpaceSpacer(if (distribution == FlexDistribution.SPACE_AROUND) 2f else 1f)
                    }
                }
            }
        }
    }
}
