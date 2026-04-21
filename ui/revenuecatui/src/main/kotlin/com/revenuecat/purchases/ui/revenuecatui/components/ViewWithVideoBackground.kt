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
