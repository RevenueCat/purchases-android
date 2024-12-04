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
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
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
                urlString = style.urls.urlsForCurrentTheme.webp.toString(),
                modifier = Modifier
                    .size(style.size)
                    .applyIfNotNull(style.colorStyle) { colorStyle ->
                        drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                when (colorStyle) {
                                    is ColorStyle.Solid -> drawRect(colorStyle.color)
                                    is ColorStyle.Gradient -> drawRect(colorStyle.brush)
                                }
                            }
                        }
                    },
                placeholderUrlString = style.urls.urlsForCurrentTheme.webpLowRes.toString(),
                contentScale = style.contentScale,
            )
        }
    }
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

@Suppress("MagicNumber")
@Preview
@Composable
private fun ImageComponentView_Preview_LinearGradient() {
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                colorStyle = ColorStyle.Gradient(
                    Brush.verticalGradient(
                        Pair(0f, ComposeColor(Color.parseColor("#88FF0000"))),
                        Pair(0.5f, ComposeColor(Color.parseColor("#8800FF00"))),
                        Pair(1f, ComposeColor(Color.parseColor("#880000FF"))),
                    ),
                ),
            ),
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun ImageComponentView_Preview_RadialGradient() {
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                colorStyle = ColorStyle.Gradient(
                    Brush.radialGradient(
                        Pair(0f, ComposeColor(Color.parseColor("#88FF0000"))),
                        Pair(0.5f, ComposeColor(Color.parseColor("#8800FF00"))),
                        Pair(1f, ComposeColor(Color.parseColor("#880000FF"))),
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
    lowResURL: URL = URL("https://assets.pawwalls.com/954459_1701163461.jpg"),
    visible: Boolean = true,
    size: Size = Size(width = SizeConstraint.Fixed(400u), height = SizeConstraint.Fixed(400u)),
    contentScale: ContentScale = ContentScale.Crop,
    colorStyle: ColorStyle? = null,
) = ImageComponentStyle(
    visible = visible,
    themeImageUrls = ThemeImageUrls(light = ImageUrls(url, url, lowResURL, 200u, 200u)),
    size = size,
    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 20.dp),
    colorStyle = colorStyle,
    contentScale = contentScale,
)
