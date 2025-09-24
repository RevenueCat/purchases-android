package com.revenuecat.purchases.ui.revenuecatui.components.video

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
    repository: FileRepository = DefaultFileRepository(LocalContext.current),
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
            var fallbackImageViewStyle: ImageComponentStyle? by rememberSaveable {
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
                            overrides = emptyList(), // TODO
                            ignoreTopWindowInsets = style.ignoreTopWindowInsets,
                        ),
                    )
                } else {
                    mutableStateOf(null)
                }
            }

            var videoUrl by rememberSaveable { mutableStateOf(repository.getFile(videoState.videoUrls.url)) }

            // If the low res and normal resolution files were not yet found on disk
            // then we attempt to finish the download by calling the following method.
            // this method will share the async task that the Predownload started
            // if it didn't error out, expediting the download time and reducing the memory
            // footprint of paywalls
            suspend fun fetchVideoUrl(withUrgency: Boolean) {
                try {
                    if (withUrgency) {
                        videoUrl = videoState.videoUrls.urlLowRes?.toString()?.let(::URI)
                    }

                    val url = repository.generateOrGetCachedFileURL(videoState.videoUrls.url)
                    videoUrl = url
                    fallbackImageViewStyle = null
                } catch (_: Exception) {
                    // This is a fallback state where it is possible that we render the video on top of the image view
                    // this may result in some paywalls not looking so good depending on the fallback image they used
                    // and the styles applied to the video component
                    videoUrl = videoState.videoUrls.url.toString().let(::URI)
                }
            }

            // If the full size video wasn't found on disk try the low res
            if (videoUrl == null) {
                if (videoState.videoUrls.urlLowRes != null) {
                    videoUrl = repository.getFile(videoState.videoUrls.urlLowRes!!)
                    // if the low res was found, we should fetch the better one
                    LaunchedEffect(Unit) {
                        fetchVideoUrl(withUrgency = false)
                    }
                }
            }

            // If both of the video files are not found on disk
            if (videoUrl == null) {
                LaunchedEffect(Unit) {
                    fetchVideoUrl(withUrgency = fallbackImageViewStyle == null)
                }
            } else {
                fallbackImageViewStyle = null
            }

            fallbackImageViewStyle?.let { ImageComponentView(it, state, modifier) }

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
