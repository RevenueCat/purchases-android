package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background

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
                    model = imageUrls.webp.toString(),
                    placeholder = rememberAsyncImagePainter(
                        model = imageUrls.webpLowRes.toString(),
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

@JvmSynthetic
@Composable
internal fun ColorScheme.toBackgroundStyle(): BackgroundStyle =
    BackgroundStyle.Color(color = toColorStyle())

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
                                    percent = 0.5f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = Color.Blue.toArgb(),
                                    percent = 1f,
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
                                    percent = 0.5f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = Color.Blue.toArgb(),
                                    percent = 1f,
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
