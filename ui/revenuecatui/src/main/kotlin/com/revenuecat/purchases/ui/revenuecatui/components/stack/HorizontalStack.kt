@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A horizontal stack of components which properly handles the arrangement of items.
 */
@Composable
internal fun HorizontalStack(
    dimension: Dimension.Horizontal,
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: HorizontalStackScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = dimension.alignment.toAlignment(),
        horizontalArrangement = dimension.distribution.toHorizontalArrangement(
            spacing = spacing,
        ),
    ) {
        val scope = HorizontalStackScopeImpl().apply(content)
        scope.rowContent(this)
    }
}

internal interface HorizontalStackScope {
    fun items(
        items: List<ComponentStyle>,
        itemContent: @Composable RowScope.(index: Int, item: ComponentStyle) -> Unit,
    )
}

private class HorizontalStackScopeImpl : HorizontalStackScope {
    var rowContent: @Composable RowScope.() -> Unit = {}

    override fun items(
        items: List<ComponentStyle>,
        itemContent: @Composable RowScope.(index: Int, item: ComponentStyle) -> Unit,
    ) {
        rowContent = {
            items.forEachIndexed { index, item ->
                itemContent(index, item)
            }
        }
    }
}
