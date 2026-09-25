@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A vertical stack of components which properly handles the arrangement of items.
 */
@Composable
internal fun VerticalStack(
    dimension: Dimension.Vertical,
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: VerticalStackScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = dimension.distribution.toVerticalArrangement(
            spacing = spacing,
        ),
        horizontalAlignment = dimension.alignment.toAlignment(),
    ) {
        val scope = VerticalStackScopeImpl().apply(content)
        scope.columnContent(this)
    }
}

internal interface VerticalStackScope {
    fun items(
        items: List<ComponentStyle>,
        itemContent: @Composable ColumnScope.(index: Int, item: ComponentStyle) -> Unit,
    )
}

private class VerticalStackScopeImpl : VerticalStackScope {
    var columnContent: @Composable ColumnScope.() -> Unit = {}

    override fun items(
        items: List<ComponentStyle>,
        itemContent: @Composable ColumnScope.(index: Int, item: ComponentStyle) -> Unit,
    ) {
        columnContent = {
            items.forEachIndexed { index, item ->
                itemContent(index, item)
            }
        }
    }
}
