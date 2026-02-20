package com.revenuecat.purchases.ui.revenuecatui.components.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URI

@Suppress("CyclomaticComplexMethod", "LongMethod", "ModifierNotUsedAtRoot", "ModifierReused")
@JvmSynthetic
@Composable
internal fun VideoComponentView(
    style: VideoComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    repository: FileRepository = Purchases.sharedInstance.fileRepository,
) {
    val videoState = rememberUpdatedVideoComponentState(style, state)

    if (videoState.visible) {
        val overlay = videoState.overlay?.forCurrentTheme
        val borderStyle = videoState.border?.let { rememberBorderStyle(border = it) }
        val shadowStyle = videoState.shadow?.let { rememberShadowStyle(shadow = it) }
        val composeShape by remember(videoState.shape) { derivedStateOf { videoState.shape ?: RectangleShape } }

        var isVisible by remember { mutableStateOf(false) }
        var videoReady by remember(isVisible) { mutableStateOf(false) }
        val view = LocalView.current

        // Fallback style - always available so it shows while scrolling
        val fallbackStyle = remember(style.fallbackSources) {
            style.fallbackSources?.let { sources ->
                ImageComponentStyle(
                    sources = sources,
                    visible = style.visible,
                    size = style.size,
                    padding = PaddingValues(0.dp),
                    margin = PaddingValues(0.dp),
                    shape = null,
                    border = null,
                    shadow = null,
                    overlay = style.overlay,
                    contentScale = style.contentScale,
                    rcPackage = style.rcPackage,
                    tabIndex = style.tabIndex,
                    overrides = emptyList(),
                    ignoreTopWindowInsets = style.ignoreTopWindowInsets,
                )
            }
        }

        // Get video URL - only when visible to avoid initializing all videos at once
        val videoUrl = if (isVisible) {
            rememberVideoContentState(videoState.videoUrls, repository)
        } else {
            null
        }

        Box(
            modifier = modifier
                .size(videoState.sizePlusMargin)
                .applyIfNotNull(videoState.marginAdjustedAspectRatio) { aspectRatio(it) }
                .padding(videoState.margin)
                .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
                .clip(composeShape)
                .applyIfNotNull(borderStyle) { border(it, composeShape).padding(it.width) }
                .onGloballyPositioned { coordinates ->
                    isVisible = coordinates.boundsInWindow().isVisibleInViewport(view.width, view.height)
                },
        ) {
            // VideoView renders first (underneath)
            if (isVisible && videoUrl != null) {
                VideoView(
                    videoUri = videoUrl.toString(),
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
                    onReady = { videoReady = true },
                )
            }

            // Fallback shows on top until video's first frame is rendered
            if (fallbackStyle != null && !videoReady) {
                ImageComponentView(fallbackStyle, state)
            }
        }
    }
}

@JvmSynthetic
internal fun Rect.isVisibleInViewport(viewportWidth: Int, viewportHeight: Int): Boolean {
    return right > 0 && bottom > 0 && left < viewportWidth && top < viewportHeight
}

@Composable
private fun rememberVideoContentState(
    videoUrls: VideoUrls,
    repository: FileRepository,
): URI? {
    // Check high-res cache synchronously
    val cachedHighRes = remember(videoUrls.url) {
        repository.getFile(videoUrls.url, videoUrls.checksum)
    }

    var videoUrl by remember(videoUrls.url) { mutableStateOf(cachedHighRes) }

    if (cachedHighRes == null) {
        // Not cached — concurrently fetch both resolutions
        LaunchedEffect(videoUrls.url) {
            val resolved = resolveVideoUrl(videoUrls, repository)

            if (resolved != null) {
                videoUrl = resolved
            }
        }
    }

    return videoUrl
}
internal suspend fun resolveVideoUrl(
    videoUrls: VideoUrls,
    repository: FileRepository,
): URI? = coroutineScope {
    val highResDeferred = async {
        runCatching {
            repository.generateOrGetCachedFileURL(videoUrls.url, videoUrls.checksum)
        }.getOrNull()
    }
    val lowResDeferred = videoUrls.urlLowRes
        ?.takeIf { it != videoUrls.url }
        ?.let {
            async {
                runCatching {
                    repository.generateOrGetCachedFileURL(it, videoUrls.checksumLowRes)
                }.getOrNull()
            }
        }

    val highResResult = highResDeferred.await()

    highResResult ?: lowResDeferred?.await()
}
