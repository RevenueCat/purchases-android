package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.getImageLoaderTyped
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalPreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.getPreviewPlaceholderBlocking
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull

/**
 * Ready to use background properties for the current theme.
 */
internal sealed interface BackgroundStyle {
    @JvmInline
    value class Color(@get:JvmSynthetic val color: ColorStyle) : BackgroundStyle

    data class Image(
        @get:JvmSynthetic val painter: Painter,
        @get:JvmSynthetic val contentScale: ContentScale,
        @get:JvmSynthetic val colorOverlay: ColorStyle?,
    ) : BackgroundStyle

    data class Video(
        @get:JvmSynthetic val sources: ThemeVideoUrls,
        @get:JvmSynthetic val fallbackImage: ThemeImageUrls,
        @get:JvmSynthetic val loop: Boolean,
        @get:JvmSynthetic val muteAudio: Boolean,
        @get:JvmSynthetic val contentScale: ContentScale,
        @get:JvmSynthetic val colorOverlay: ColorStyles?,
    ) : BackgroundStyle
}

/**
 * Background properties with resolved colors.
 */
@Stable
internal sealed interface BackgroundStyles {
    @JvmInline
    @Immutable
    value class Color(@get:JvmSynthetic val color: ColorStyles) : BackgroundStyles

    @Immutable
    data class Image(
        @get:JvmSynthetic val sources: ThemeImageUrls,
        @get:JvmSynthetic val contentScale: ContentScale,
        @get:JvmSynthetic val colorOverlay: ColorStyles?,
    ) : BackgroundStyles

    @Immutable
    data class Video(
        @get:JvmSynthetic val sources: ThemeVideoUrls,
        @get:JvmSynthetic val fallbackImage: ThemeImageUrls,
        @get:JvmSynthetic val loop: Boolean,
        @get:JvmSynthetic val muteAudio: Boolean,
        @get:JvmSynthetic val contentScale: ContentScale,
        @get:JvmSynthetic val colorOverlay: ColorStyles?,
    ) : BackgroundStyles
}

@JvmSynthetic
internal fun Background.toBackgroundStyles(
    aliases: Map<ColorAlias, ColorScheme>,
): Result<BackgroundStyles, NonEmptyList<PaywallValidationError>> =
    when (this) {
        is Background.Color ->
            value
                .toColorStyles(aliases = aliases)
                .map { color -> BackgroundStyles.Color(color) }

        is Background.Image ->
            colorOverlay
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull()
                .map { colorOverlay ->
                    BackgroundStyles.Image(
                        sources = value,
                        contentScale = fitMode.toContentScale(),
                        colorOverlay = colorOverlay,
                    )
                }

        is Background.Video ->
            colorOverlay
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull()
                .map { colorOverlay ->
                    BackgroundStyles.Video(
                        sources = value,
                        fallbackImage = fallbackImage,
                        loop = loop,
                        muteAudio = muteAudio,
                        contentScale = fitMode.toContentScale(),
                        colorOverlay = colorOverlay,
                    )
                }

        is Background.Unknown -> Result.Error(
            nonEmptyListOf(PaywallValidationError.UnsupportedBackgroundType(background = this)),
        )
    }

@Stable
@Composable
@JvmSynthetic
internal fun rememberBackgroundStyle(background: BackgroundStyles): BackgroundStyle =
    when (background) {
        is BackgroundStyles.Color -> {
            val color = background.color.forCurrentTheme
            remember(background, color) {
                BackgroundStyle.Color(color = color)
            }
        }
        is BackgroundStyles.Image -> {
            val colorOverlay = background.colorOverlay?.forCurrentTheme
            val source = background.sources.urlsForCurrentTheme
            val painter = rememberAsyncImagePainter(source, background.contentScale)
            remember(colorOverlay, source, painter) {
                BackgroundStyle.Image(
                    painter = painter,
                    contentScale = background.contentScale,
                    colorOverlay = colorOverlay,
                )
            }
        }
        is BackgroundStyles.Video -> {
            remember(background) {
                BackgroundStyle.Video(
                    sources = background.sources,
                    fallbackImage = background.fallbackImage,
                    loop = background.loop,
                    muteAudio = background.muteAudio,
                    contentScale = background.contentScale,
                    colorOverlay = background.colorOverlay,
                )
            }
        }
    }

@Stable
@Composable
private fun rememberAsyncImagePainter(imageUrls: ImageUrls, contentScale: ContentScale): AsyncImagePainter {
    var cachePolicy by remember { mutableStateOf(CachePolicy.ENABLED) }
    val context = LocalContext.current
    val previewImageLoader = LocalPreviewImageLoader.current
    val isInPreviewMode = isInPreviewMode()
    val imageLoader = previewImageLoader.takeIf { isInPreviewMode } ?: remember(context) {
        Purchases.getImageLoaderTyped(context.applicationContext)
    }
    val imageRequest = remember(context, imageUrls.webp, cachePolicy) {
        getImageRequest(context, imageUrls.webp.toString(), cachePolicy)
    }
    return rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = imageLoader,
        placeholder = if (isInPreviewMode && previewImageLoader != null) {
            imageLoader.getPreviewPlaceholderBlocking(imageRequest)
        } else {
            rememberAsyncImagePainter(
                model = getImageRequest(context, imageUrls.webpLowRes.toString(), cachePolicy),
                imageLoader = imageLoader,
                error = null,
                fallback = null,
                contentScale = contentScale,
            )
        },
        error = null,
        fallback = null,
        onError = {
            Logger.w("AsyncImagePainter failed to load. Will try again disabling cache")
            cachePolicy = CachePolicy.WRITE_ONLY
        },
        contentScale = contentScale,
    )
}

private fun getImageRequest(context: Context, url: String, cachePolicy: CachePolicy): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .diskCachePolicy(cachePolicy)
        .memoryCachePolicy(cachePolicy)
        .build()

@Preview
@Composable
private fun Background_Preview_ColorHex() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(BackgroundStyle.Color(ColorStyle.Solid(Color.Red))),
    )
}

@Preview
@Composable
private fun Background_Preview_ColorGradientLinear() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(
                BackgroundStyle.Color(
                    ColorInfo.Gradient.Linear(
                        degrees = 90f,
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.Red.toArgb(),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.Green.toArgb(),
                                percent = 50f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.Blue.toArgb(),
                                percent = 100f,
                            ),
                        ),
                    ).toColorStyle(),
                ),
            ),
    )
}

@Preview
@Composable
private fun Background_Preview_ColorGradientRadial() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(
                BackgroundStyle.Color(
                    ColorInfo.Gradient.Radial(
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.Red.toArgb(),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.Green.toArgb(),
                                percent = 50f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.Blue.toArgb(),
                                percent = 100f,
                            ),
                        ),
                    ).toColorStyle(),
                ),
            ),
    )
}

// We cannot use a network image in Compose previews, so we don't have a preview for Background.Image. Instead, we have
// some tests in BackgroundTests.
