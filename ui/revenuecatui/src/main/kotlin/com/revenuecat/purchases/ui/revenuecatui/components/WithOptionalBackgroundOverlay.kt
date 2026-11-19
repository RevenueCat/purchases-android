package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Composable
@Suppress("LongParameterList")
internal fun WithOptionalBackgroundOverlay(
    state: PaywallState.Loaded.Components,
    background: BackgroundStyle?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    size: Size? = null,
    content: @Composable (Modifier) -> Unit,
) {
    when {
        background is BackgroundStyle.Video -> {
            ViewWithVideoBackground(
                state = state,
                background = background,
                shape = shape,
                modifier = modifier,
                size = size,
            ) { minimums ->
                content(minimums)
            }
        }
        background is BackgroundStyle.Image && background.colorOverlay != null -> {
            // Image backgrounds with color overlays need the overlay to cover the full container,
            // not just the image bounds. This matches the web builder behavior where overlays
            // cover 100% of the viewport.
            BoxWithUnboundedFillMinimums(size = size, modifier = modifier) { minimums ->
                // Render overlay BEHIND content but in front of background image (applied via modifier)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(background.colorOverlay, shape),
                )
                content(minimums)
            }
        }
        else -> {
            content(Modifier)
        }
    }
}
