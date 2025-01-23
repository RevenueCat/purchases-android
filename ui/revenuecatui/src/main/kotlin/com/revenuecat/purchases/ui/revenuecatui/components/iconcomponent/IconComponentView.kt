@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL

@JvmSynthetic
@Composable
internal fun IconComponentView(
    style: IconComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    previewImageLoader: ImageLoader? = null,
) {
    val iconState = rememberUpdatedIconComponentState(
        style = style,
        paywallState = state,
    )

    if (!iconState.visible) {
        return
    }

    val borderStyle = iconState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = iconState.shadow?.let { rememberShadowStyle(shadow = it) }
    val composeShape by remember(iconState.shape) { derivedStateOf { iconState.shape ?: RectangleShape } }
    val backgroundColor = iconState.backgroundColorStyles?.forCurrentTheme
    val tintColor = iconState.tintColor?.forCurrentTheme
    val colorFilter by remember(tintColor) {
        derivedStateOf {
            // TODO Support gradient tints
            (tintColor as? ColorStyle.Solid)?.let { ColorFilter.tint(it.color) }
        }
    }

    RemoteImage(
        urlString = iconState.url,
        modifier = modifier
            .size(iconState.size)
            .padding(iconState.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
            .applyIfNotNull(backgroundColor) { background(it, composeShape) }
            .clip(composeShape)
            .applyIfNotNull(borderStyle) { border(it, composeShape) }
            .padding(iconState.padding),
        colorFilter = colorFilter,
        previewImageLoader = previewImageLoader,
    )
}

@Preview
@Composable
private fun IconComponentView_Preview() {
    Box(modifier = Modifier.background(Color.LightGray)) {
        IconComponentView(
            style = previewIconComponentStyle(
                size = Size(
                    width = SizeConstraint.Fixed(200u),
                    height = SizeConstraint.Fixed(200u),
                ),
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewIconComponentStyle(
    size: Size,
    color: ColorStyles = ColorStyles(
        light = ColorStyle.Solid(Color.Cyan),
    ),
    backgroundColor: ColorStyles = ColorStyles(
        light = ColorStyle.Solid(Color.Red),
    ),
    paddingValues: PaddingValues = PaddingValues(10.dp),
    marginValues: PaddingValues = PaddingValues(10.dp),
    border: Border? = Border(
        width = 2.0,
        color = ColorScheme(
            light = ColorInfo.Hex(
                Color.Cyan.toArgb(),
            ),
        ),
    ),
    shadow: Shadow? = Shadow(
        color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
        radius = 10.0,
        x = 0.0,
        y = 3.0,
    ),
    shape: MaskShape = MaskShape.Circle,
) = IconComponentStyle(
    baseUrl = "https://example.com",
    iconName = "test-icon-name",
    formats = IconComponent.Formats(
        webp = "test-webp",
    ),
    size = size,
    color = color,
    padding = paddingValues,
    margin = marginValues,
    iconBackground = IconComponentStyle.Background(
        shape = shape,
        border = border,
        shadow = shadow,
        color = backgroundColor,
    ),
    rcPackage = null,
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
                    ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                ),
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
        paywallComponents = Offering.PaywallComponents(UiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    return offering.toComponentsPaywallState(
        validationResult = validated,
        activelySubscribedProductIds = emptySet(),
        purchasedNonSubscriptionProductIds = emptySet(),
        storefrontCountryCode = null,
    )
}

@Composable
private fun previewImageLoader(
    @DrawableRes resource: Int = R.drawable.android,
): ImageLoader {
    val context = LocalContext.current
    return ImageLoader.Builder(context)
        .components {
            add { chain ->
                SuccessResult(
                    drawable = context.getDrawable(resource)!!,
                    request = chain.request,
                    dataSource = DataSource.MEMORY,
                )
            }
        }
        .build()
}
