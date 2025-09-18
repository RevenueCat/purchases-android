package com.revenuecat.purchases.ui.revenuecatui.components.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.aspectRatio
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull

@JvmSynthetic
@Composable
internal fun VideoComponentView(
    style: VideoComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val videoState = rememberUpdatedVideoComponentState(style, state)

    if (videoState.visible) {
        val overlay = videoState.overlay?.forCurrentTheme
        val borderStyle = videoState.border?.let { rememberBorderStyle(border = it) }
        val shadowStyle = videoState.shadow?.let { rememberShadowStyle(shadow = it) }
        val composeShape by remember(videoState.shape) { derivedStateOf { videoState.shape ?: RectangleShape } }

        Box(
            modifier = modifier
                .size(videoState.sizePlusMargin)
                .applyIfNotNull(videoState.marginAdjustedAspectRatio) { aspectRatio(it) }
                .padding(videoState.margin)
                .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
                .clip(composeShape)
                .applyIfNotNull(borderStyle) { border(it, composeShape).padding(it.width) },
        ) {
            // TO DO: use image component view as the fallback
//            ImageComponentView()
            VideoView(
                videoUri = videoState.videoUrls.url.toString(),
                modifier = Modifier
                    .size(videoState.size)
                    .applyIfNotNull(videoState.aspectRatio) { aspectRatio(it) }
                    .applyIfNotNull(overlay) { overlay(it) }
                    .padding(videoState.padding),
                showControls = style.showControls,
                autoPlay = style.autoplay,
                loop = style.loop,
                muteAudio = style.muteAudio,
                contentScale = style.contentScale,
            )
        }
    }
}
