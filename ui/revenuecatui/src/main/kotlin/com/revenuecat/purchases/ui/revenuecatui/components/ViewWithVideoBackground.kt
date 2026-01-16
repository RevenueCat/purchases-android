package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.video.VideoComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf

@Composable
internal fun ViewWithVideoBackground(
    state: PaywallState.Loaded.Components,
    background: BackgroundStyle?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        when (val background = background) {
            is BackgroundStyle.Video -> {
                val videoComponent = VideoComponentStyle(
                    sources = nonEmptyMapOf(state.locale.platformLocale.toLocaleId() to background.sources),
                    fallbackSources = nonEmptyMapOf(
                        state.locale.platformLocale.toLocaleId() to background.fallbackImage,
                    ),
                    visible = true,
                    showControls = false,
                    autoplay = true,
                    loop = background.loop,
                    muteAudio = background.muteAudio,
                    size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
                    shape = shape,
                    overlay = background.colorOverlay,
                    contentScale = background.contentScale,
                    rcPackage = null,
                    tabIndex = null,
                    ignoreTopWindowInsets = true,
                    overrides = emptyList(),
                    padding = PaddingValues(),
                    margin = PaddingValues(),
                    border = null,
                    shadow = null,
                )

                VideoComponentView(
                    style = videoComponent,
                    state = state,
                    modifier = Modifier.matchParentSize(),
                )
            }

            else -> {}
        }
        content()
    }
}

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
