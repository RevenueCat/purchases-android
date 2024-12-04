package com.revenuecat.purchases.ui.revenuecatui.components.image

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import java.net.URL
import androidx.compose.ui.graphics.Color as ComposeColor

@Suppress("SpreadOperator", "LongParameterList")
@JvmSynthetic
@Composable
internal fun ImageComponentView(
    style: ImageComponentStyle,
    modifier: Modifier = Modifier,
) {
    if (style.visible) {
        Box(modifier = modifier.applyIfNotNull(style.shape) { clip(it) }) {
            RemoteImage(
                urlString = style.urlToUse().toString(),
                modifier = Modifier
                    .size(style.size)
                    .applyIfNotNull(style.gradientColors()) { gradientColors ->
                        drawWithCache {
                            val gradientBrush: Brush? = when (gradientColors) {
                                is ColorInfo.Gradient.Radial -> {
                                    Brush.radialGradient(*gradientColors.points.toColorStops())
                                }
                                is ColorInfo.Gradient.Linear -> {
                                    // TODO-PAYWALLS-V2: Support degrees
                                    Brush.verticalGradient(*gradientColors.points.toColorStops())
                                }
                                else -> null
                            }
                            onDrawWithContent {
                                drawContent()
                                gradientBrush?.let {
                                    drawRect(it)
                                }
                            }
                        }
                    },
                placeholderUrlString = style.lowResUrlToUse()?.toString(),
                contentScale = style.contentScale,
            )
        }
    }
}

private fun List<ColorInfo.Gradient.Point>.toColorStops(): Array<Pair<Float, ComposeColor>> {
    return map { point ->
        point.percent to ComposeColor(point.color)
    }.toTypedArray()
}

@Preview
@Composable
private fun ImageComponentView_Preview_Default() {
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(),
        )
    }
}

@Preview
@Composable
private fun ImageComponentView_Preview_LinearGradient() {
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                gradientColors = ColorInfo.Gradient.Linear(
                    degrees = 45f,
                    points = listOf(
                        ColorInfo.Gradient.Point(color = Color.parseColor("#88FF0000"), percent = 0f),
                        ColorInfo.Gradient.Point(color = Color.parseColor("#8800FF00"), percent = 0.5f),
                        ColorInfo.Gradient.Point(color = Color.parseColor("#880000FF"), percent = 1f),
                    ),
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun ImageComponentView_Preview_RadialGradient() {
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                gradientColors = ColorInfo.Gradient.Radial(
                    points = listOf(
                        ColorInfo.Gradient.Point(color = Color.parseColor("#88FF0000"), percent = 0f),
                        ColorInfo.Gradient.Point(color = Color.parseColor("#8800FF00"), percent = 0.5f),
                        ColorInfo.Gradient.Point(color = Color.parseColor("#880000FF"), percent = 1f),
                    ),
                ),
            ),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewImageComponentStyle(
    url: URL = URL("https://sample-videos.com/img/Sample-jpg-image-5mb.jpg"),
    lowResURL: URL? = URL("https://assets.pawwalls.com/954459_1701163461.jpg"),
    visible: Boolean = true,
    size: Size = Size(width = SizeConstraint.Fixed(400u), height = SizeConstraint.Fixed(400u)),
    contentScale: ContentScale = ContentScale.Crop,
    gradientColors: ColorInfo.Gradient? = null,
) = ImageComponentStyle(
    visible = visible,
    url = url,
    lowResURL = lowResURL,
    darkURL = null,
    darkLowResURL = null,
    size = size,
    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 20.dp),
    gradientColors = gradientColors,
    darkGradientColors = null,
    contentScale = contentScale,
)
