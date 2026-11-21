@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.stack.preserveUnboundedFillMinimums

/** Preserve scroll minimums for main content without stretching badge or overlay siblings. */
@Composable
internal fun BoxWithUnboundedFillMinimums(
    modifier: Modifier = Modifier,
    size: Size? = null,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    if (size == null) {
        Box(modifier) { content(Modifier) }
    } else {
        BoxWithConstraints(modifier) {
            val parent = constraints
            content(
                Modifier.layout { measurable, child ->
                    val placeable = measurable.measure(child.preserveUnboundedFillMinimums(size, parent, this))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
            )
        }
    }
}
