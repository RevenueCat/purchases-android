package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
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
}

/**
 * Background properties with resolved colors.
 */
internal sealed interface BackgroundStyles {
    @JvmInline
    value class Color(@get:JvmSynthetic val color: ColorStyles) : BackgroundStyles

    data class Image(
        @get:JvmSynthetic val sources: ThemeImageUrls,
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
    }

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
    }

@JvmSynthetic
@Composable
internal fun Background.toBackgroundStyle(): BackgroundStyle =
    when (this) {
        is Background.Color -> BackgroundStyle.Color(color = value.toColorStyle())
        is Background.Image -> {
            val imageUrls = value.urlsForCurrentTheme
            val contentScale = fitMode.toContentScale()
            BackgroundStyle.Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrls.webp.toString(),
                    placeholder = rememberAsyncImagePainter(
                        model = imageUrls.webpLowRes.toString(),
                        error = null,
                        fallback = null,
                        contentScale = contentScale,
                    ),
                    error = null,
                    fallback = null,
                    contentScale = contentScale,
                ),
                contentScale = contentScale,
                colorOverlay = colorOverlay?.toColorStyle(),
            )
        }
    }

@Composable
private fun rememberAsyncImagePainter(imageUrls: ImageUrls, contentScale: ContentScale): AsyncImagePainter =
    rememberAsyncImagePainter(
        model = imageUrls.webp.toString(),
        placeholder = rememberAsyncImagePainter(
            model = imageUrls.webpLowRes.toString(),
            error = null,
            fallback = null,
            contentScale = contentScale,
        ),
        error = null,
        fallback = null,
        contentScale = contentScale,
    )

@Preview
@Composable
private fun Background_Preview_ColorHex() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(
                Background.Color(
                    ColorScheme(
                        light = ColorInfo.Hex(Color.Red.toArgb()),
                    ),
                ).toBackgroundStyle(),
            ),
    )
}

@Preview
@Composable
private fun Background_Preview_ColorGradientLinear() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(
                Background.Color(
                    ColorScheme(
                        light = ColorInfo.Gradient.Linear(
                            degrees = 0f,
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
                        ),
                    ),
                ).toBackgroundStyle(),
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
                Background.Color(
                    ColorScheme(
                        light = ColorInfo.Gradient.Radial(
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
                        ),
                    ),
                ).toBackgroundStyle(),
            ),
    )
}

// We cannot use a network image in Compose previews, so we don't have a preview for Background.Image. Instead, we have
// some tests in BackgroundTests.
