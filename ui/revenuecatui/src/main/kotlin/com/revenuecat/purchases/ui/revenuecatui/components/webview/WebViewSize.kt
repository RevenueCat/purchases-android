@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import kotlin.math.roundToInt

/**
 * AndroidView turns an infinite maximum into an UNSPECIFIED measure spec, losing even a nonzero
 * Compose minimum. A Fill web view with a viewport minimum needs a finite spec to size its native
 * FrameLayout and WebView. Fit axes and unbounded axes with no available space keep their fallback.
 */
internal fun Modifier.fillUnboundedAxisFromMinimum(size: Size): Modifier = layout { measurable, constraints ->
    val widthMinimum = maxOf(
        constraints.minWidth,
        (size.width as? Fill)?.min?.let { (it.toDouble() * density).roundToInt() } ?: 0,
    )
    val heightMinimum = maxOf(
        constraints.minHeight,
        (size.height as? Fill)?.min?.let { (it.toDouble() * density).roundToInt() } ?: 0,
    )
    val maxWidth = if (size.width is Fill && !constraints.hasBoundedWidth && widthMinimum > 0) {
        widthMinimum
    } else {
        constraints.maxWidth
    }
    val maxHeight = if (size.height is Fill && !constraints.hasBoundedHeight && heightMinimum > 0) {
        heightMinimum
    } else {
        constraints.maxHeight
    }
    val placeable = measurable.measure(constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight))
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}
