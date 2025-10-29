@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.video

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.storage.FileRepository
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.getImageLoaderTyped
import java.net.URI

// Extension function to get the appropriate video URLs for the current theme
private val ThemeVideoUrls.urlsForCurrentTheme: VideoUrls
    @ReadOnlyComposable @Composable
    get() = if (isSystemInDarkTheme()) dark ?: light else light

/**
 * A simplified video view optimized for use as a background.
 * Automatically plays, loops, and doesn't show controls.
 * Falls back to an image if video loading fails.
 */
@Suppress("LongParameterList")
@Composable
internal fun VideoBackgroundView(
    sources: ThemeVideoUrls,
    fallbackImage: ThemeImageUrls,
    loop: Boolean,
    muteAudio: Boolean,
    contentScale: ContentScale,
    colorOverlay: ColorStyle?,
    shape: Shape = RectangleShape,
    repository: FileRepository = Purchases.sharedInstance.fileRepository,
) {
    val videoUrls = sources.urlsForCurrentTheme
    val fallbackImageUrls = fallbackImage.urlsForCurrentTheme

    var videoUrl by remember(videoUrls) { mutableStateOf<URI?>(null) }
    var showFallback by remember(videoUrls) { mutableStateOf(false) }

    // Attempt to load video file
    LaunchedEffect(videoUrls) {
        try {
            val url = repository.generateOrGetCachedFileURL(
                url = videoUrls.url,
                checksum = videoUrls.checksum,
            )
            videoUrl = url
            showFallback = false
        } catch (e: Exception) {
            showFallback = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape),
    ) {
        when {
            videoUrl != null && !showFallback -> {
                // Show video
                VideoView(
                    videoUri = videoUrl.toString(),
                    modifier = Modifier
                        .fillMaxSize()
                        .applyIfNotNull(colorOverlay, Modifier::overlay),
                    showControls = false,
                    autoPlay = true,
                    loop = loop,
                    muteAudio = muteAudio,
                    contentScale = contentScale,
                )
            }
            else -> {
                // Show fallback image
                FallbackImageView(
                    imageUrls = fallbackImageUrls,
                    contentScale = contentScale,
                    colorOverlay = colorOverlay,
                )
            }
        }
    }
}

/**
 * Fallback image view for when video fails to load or is still loading
 */
@Composable
private fun FallbackImageView(
    imageUrls: ImageUrls,
    contentScale: ContentScale,
    colorOverlay: ColorStyle?,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        Purchases.getImageLoaderTyped(context.applicationContext)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrls.webp.toString())
                .build(),
            contentDescription = null,
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxSize()
                .applyIfNotNull(colorOverlay, Modifier::overlay),
            contentScale = contentScale,
        )
    }
}
