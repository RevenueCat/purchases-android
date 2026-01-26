package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Composable
internal fun WithOptionalBackgroundOverlay(
    state: PaywallState.Loaded.Components,
    background: BackgroundStyle?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit,
) {
    when {
        background is BackgroundStyle.Video -> {
            ViewWithVideoBackground(
                state = state,
                background = background,
                shape = shape,
                modifier = modifier,
            ) {
                content()
            }
        }
        background is BackgroundStyle.Image && background.colorOverlay != null -> {
            // Image backgrounds with color overlays need the overlay to cover the full container,
            // not just the image bounds. This matches the web builder behavior where overlays
            // cover 100% of the viewport.
            Box(modifier = modifier) {
                // Render overlay BEHIND content but in front of background image (applied via modifier)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(background.colorOverlay, shape),
                )
                content()
            }
        }
        else -> {
            content()
        }
    }
}
