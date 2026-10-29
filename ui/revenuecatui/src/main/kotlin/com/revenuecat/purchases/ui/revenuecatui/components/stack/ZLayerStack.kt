@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

/**
 * A layer stack keeps the viewport minimum for Fill-height children inside an outer scroll.
 * A Box drops that minimum, leaving Fill children with no height to fill. The maximum stays
 * unbounded so content taller than the viewport can still grow and scroll.
 */
@Composable
internal fun ZLayerStack(
    items: List<ComponentStyle>,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: ComponentStyle, modifier: Modifier) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            items.forEach { item ->
                itemContent(item, ComponentSizeParentDataModifier(item.size))
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            val fillHeight = (measurable.parentData as? ComponentSizeParentDataModifier)?.size?.height as? Fill
            val minimumHeight = if (!constraints.hasBoundedHeight && fillHeight != null) {
                val maximum = fillHeight.max?.let { maxOf(it, fillHeight.min ?: 0u).toInt().dp.roundToPx() }
                constraints.minHeight.coerceAtMost(maximum ?: constraints.minHeight)
            } else {
                0
            }
            measurable.measure(constraints.copy(minWidth = 0, minHeight = minimumHeight))
        }
        val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(placeables.maxOfOrNull { it.height } ?: 0)
        layout(width, height) {
            placeables.forEach { placeable ->
                val position = alignment.align(
                    IntSize(placeable.width, placeable.height),
                    IntSize(width, height),
                    layoutDirection,
                )
                placeable.place(position)
            }
        }
    }
}
