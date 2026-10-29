@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill

/**
 * AndroidView turns an infinite maximum into an UNSPECIFIED measure spec, losing even a nonzero
 * Compose minimum. A Fill web view with a viewport minimum needs a finite spec to size its native
 * FrameLayout and WebView. Fit axes and unbounded axes with no available space keep their fallback.
 */
internal fun Modifier.fillUnboundedAxisFromMinimum(size: Size): Modifier = layout { measurable, constraints ->
    val maxWidth = if (size.width is Fill && !constraints.hasBoundedWidth && constraints.minWidth > 0) {
        constraints.minWidth
    } else {
        constraints.maxWidth
    }
    val maxHeight = if (size.height is Fill && !constraints.hasBoundedHeight && constraints.minHeight > 0) {
        constraints.minHeight
    } else {
        constraints.maxHeight
    }
    val placeable = measurable.measure(constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight))
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}
