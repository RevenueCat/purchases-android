package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository
import com.revenuecat.purchases.ui.revenuecatui.components.image.ImageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.aspectRatio
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import java.net.URI

@Suppress("CyclomaticComplexMethod", "LongMethod", "ModifierNotUsedAtRoot", "ModifierReused")
@JvmSynthetic
@Composable
internal fun VideoComponentView(
    style: VideoComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    repository: FileRepository = remember { DefaultFileRepository(context) },
) {
    val videoState = rememberUpdatedVideoComponentState(style, state)

    if (videoState.visible) {
        val overlay = videoState.overlay?.forCurrentTheme
        val borderStyle = videoState.border?.let { rememberBorderStyle(border = it) }
        val shadowStyle = videoState.shadow?.let { rememberShadowStyle(shadow = it) }
        val composeShape by remember(videoState.shape) { derivedStateOf { videoState.shape ?: RectangleShape } }
        val (videoUrl, fallbackImageViewStyle) = rememberVideoContentState(style, videoState.videoUrls, repository)

        Box(
            modifier = modifier
                .size(videoState.sizePlusMargin)
                .applyIfNotNull(videoState.marginAdjustedAspectRatio) { aspectRatio(it) }
                .padding(videoState.margin)
                .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
                .clip(composeShape)
                .applyIfNotNull(borderStyle) { border(it, composeShape).padding(it.width) },
        ) {
            if (videoUrl == null && fallbackImageViewStyle != null) {
                ImageComponentView(fallbackImageViewStyle, state, modifier)
            }

            videoUrl?.let {
                VideoView(
                    videoUri = it.toString(),
                    modifier = Modifier
                        .size(videoState.size)
                        .applyIfNotNull(videoState.aspectRatio, Modifier::aspectRatio)
                        .applyIfNotNull(overlay, Modifier::overlay)
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
}

@Composable
private fun rememberVideoContentState(
    style: VideoComponentStyle,
    videoUrls: VideoUrls,
    repository: FileRepository,
): Pair<URI?, ImageComponentStyle?> {
    var fallbackImageViewStyle: ImageComponentStyle? by rememberSaveable(style.fallbackSources) {
        if (style.fallbackSources != null) {
            mutableStateOf(
                ImageComponentStyle(
                    sources = style.fallbackSources,
                    visible = style.visible,
                    size = style.size,
                    padding = style.padding,
                    margin = style.margin,
                    shape = style.shape,
                    border = style.border,
                    shadow = style.shadow,
                    overlay = style.overlay,
                    contentScale = style.contentScale,
                    rcPackage = style.rcPackage,
                    tabIndex = style.tabIndex,
                    overrides = emptyList(), // fallback overrides will be supplied by the video component overrides
                    ignoreTopWindowInsets = style.ignoreTopWindowInsets,
                ),
            )
        } else {
            mutableStateOf(null)
        }
    }

    var videoUrl by rememberSaveable(videoUrls.url) {
        mutableStateOf(repository.getFile(videoUrls.url))
    }

    suspend fun fetchVideoUrl(setLowResVideoURLFirst: Boolean) {
        try {
            if (setLowResVideoURLFirst) {
                videoUrl = videoUrls.urlLowRes?.toString()?.let(::URI)
            }

            val url = repository.generateOrGetCachedFileURL(videoUrls.url)
            videoUrl = url
        } catch (_: Exception) {
            videoUrl = videoUrls.url.toString().let(::URI)
        }
    }

    if (videoUrl == null) {
        videoUrls.urlLowRes
            ?.takeIf { it != videoUrls.url }
            ?.run {
                videoUrl = repository.getFile(this)
                LaunchedEffect(Unit) {
                    fetchVideoUrl(setLowResVideoURLFirst = false)
                }
            }
    }

    if (videoUrl == null) {
        LaunchedEffect(Unit) {
            fetchVideoUrl(setLowResVideoURLFirst = fallbackImageViewStyle == null)
        }
    }

    return videoUrl to fallbackImageViewStyle
}
