@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.image

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL
import androidx.compose.ui.graphics.Color as ComposeColor

@JvmSynthetic
@Composable
internal fun ImageComponentView(
    style: ImageComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    // Get an ImageComponentState that calculates the overridden properties we should use.
    val imageState = rememberUpdatedImageComponentState(
        style = style,
        paywallState = state,
        selected = selected,
    )

    if (imageState.visible) {
        val overlay = imageState.overlay?.let { rememberColorStyle(it) }
        RemoteImage(
            urlString = imageState.imageUrls.webp.toString(),
            modifier = modifier
                .size(imageState.size)
                .applyIfNotNull(overlay) { overlay(it, imageState.shape ?: RectangleShape) }
                .applyIfNotNull(imageState.shape) { clip(it) },
            placeholderUrlString = imageState.imageUrls.webpLowRes.toString(),
            contentScale = imageState.contentScale,
            imagePreview = R.drawable.android,
        )
    }
}

@Preview
@Composable
private fun ImageComponentView_Preview_Default() {
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(),
            state = previewEmptyState(),
        )
    }
}

@Preview
@Composable
private fun ImageComponentView_Preview_SmallerContainer() {
    Box(modifier = Modifier.height(200.dp).background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(),
            state = previewEmptyState(),
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
                overlay = ColorScheme(
                    light = ColorInfo.Gradient.Radial(
                        listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#88FF0000"),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#8800FF00"),
                                percent = 0.5f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#880000FF"),
                                percent = 1f,
                            ),
                        ),
                    ),
                ),
            ),
            state = previewEmptyState(),
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
                overlay = ColorScheme(
                    light = ColorInfo.Gradient.Radial(
                        listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#88FF0000"),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#8800FF00"),
                                percent = 0.5f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#880000FF"),
                                percent = 1f,
                            ),
                        ),
                    ),
                ),
            ),
            state = previewEmptyState(),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewImageComponentStyle(
    url: URL = URL("https://sample-videos.com/img/Sample-jpg-image-5mb.jpg"),
    lowResURL: URL = URL("https://assets.pawwalls.com/954459_1701163461.jpg"),
    size: Size = Size(width = SizeConstraint.Fixed(400u), height = SizeConstraint.Fit),
    contentScale: ContentScale = ContentScale.Fit,
    overlay: ColorScheme? = null,
) = ImageComponentStyle(
    sources = nonEmptyMapOf(LocaleId("en_US") to ThemeImageUrls(light = ImageUrls(url, url, lowResURL, 1000u, 1000u))),
    size = size,
    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 20.dp),
    overlay = overlay,
    contentScale = contentScale,
    overrides = null,
)

private fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one ImageComponent, but that's not needed for previews.
                stack = StackComponent(components = emptyList()),
                background = Background.Color(
                    ColorScheme(light = ColorInfo.Hex(androidx.compose.ui.graphics.Color.White.toArgb())),
                ),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(LocalizationKey("dummy") to LocalizationData.Text("dummy")),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    return offering.toComponentsPaywallState(validated)
}
