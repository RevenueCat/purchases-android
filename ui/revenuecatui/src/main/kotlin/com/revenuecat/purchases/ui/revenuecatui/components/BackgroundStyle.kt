package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme

/**
 * Ready to use background properties for the current theme.
 */
internal sealed interface BackgroundStyle {
    @JvmInline
    value class Color(@JvmSynthetic val color: ColorStyle) : BackgroundStyle

    @JvmInline
    value class Image(@JvmSynthetic val painter: Painter) : BackgroundStyle
}

@JvmSynthetic
@Composable
internal fun Background.toBackgroundStyle(): BackgroundStyle =
    when (this) {
        is Background.Color -> BackgroundStyle.Color(color = value.toColorStyle())
        is Background.Image -> {
            val imageUrls = value.urlsForCurrentTheme
            BackgroundStyle.Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrls.webp,
                    placeholder = rememberAsyncImagePainter(
                        model = imageUrls.webpLowRes,
                        error = null,
                        fallback = null,
                        contentScale = ContentScale.Crop,
                    ),
                    error = null,
                    fallback = null,
                    contentScale = ContentScale.Crop,
                ),
            )
        }
    }
