@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

@JvmSynthetic
internal fun Modifier.scrollable(
    scrollState: ScrollState,
    scrollOrientation: Orientation,
    fillViewport: Boolean = false,
): Modifier {
    val modifier = when (scrollOrientation) {
        Orientation.Vertical -> Modifier.verticalScroll(scrollState)
        Orientation.Horizontal -> Modifier.horizontalScroll(scrollState)
    }
    if (!fillViewport) return this then modifier

    // Capture the bounded viewport before scrolling unbounds its axis. Short content can then
    // use the stack's alignment across that space, while taller content still scrolls normally.
    var viewportMinimum = 0
    val vertical = scrollOrientation == Orientation.Vertical
    return this.layout { measurable, constraints ->
        viewportMinimum = when {
            vertical && constraints.hasBoundedHeight -> constraints.maxHeight
            !vertical && constraints.hasBoundedWidth -> constraints.maxWidth
            else -> 0
        }
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }.then(modifier).layout { measurable, constraints ->
        val contentConstraints = if (vertical) {
            constraints.copy(minHeight = maxOf(constraints.minHeight, viewportMinimum))
        } else {
            constraints.copy(minWidth = maxOf(constraints.minWidth, viewportMinimum))
        }
        val placeable = measurable.measure(contentConstraints)
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}
