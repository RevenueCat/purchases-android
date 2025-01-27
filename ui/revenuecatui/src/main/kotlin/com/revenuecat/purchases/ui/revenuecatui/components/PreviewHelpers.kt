@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL

@JvmSynthetic
internal fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one TextComponent, but that's not needed for previews.
                stack = StackComponent(components = emptyList()),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(
                LocalizationKey("text") to LocalizationData.Text("text"),
            ),
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

@Suppress("LongParameterList")
@JvmSynthetic
internal fun previewTextComponentStyle(
    text: String,
    color: ColorStyles = ColorStyles(ColorStyle.Solid(Color.Black)),
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.REGULAR,
    fontFamily: String? = null,
    textAlign: HorizontalAlignment = HorizontalAlignment.CENTER,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    backgroundColor: ColorStyles? = null,
    size: Size = Size(width = Fill, height = Fit),
    padding: Padding = zero,
    margin: Padding = zero,
): TextComponentStyle {
    val weight = fontWeight.toFontWeight()
    return TextComponentStyle(
        texts = nonEmptyMapOf(LocaleId("en_US") to text),
        color = color,
        fontSize = fontSize,
        fontWeight = weight,
        fontFamily = fontFamily?.let { SystemFontFamily(it, weight) },
        textAlign = textAlign.toTextAlign(),
        horizontalAlignment = horizontalAlignment.toAlignment(),
        backgroundColor = backgroundColor,
        size = size,
        padding = padding.toPaddingValues(),
        margin = margin.toPaddingValues(),
        rcPackage = null,
        overrides = null,
    )
}

@Suppress("LongParameterList")
@Composable
internal fun previewIconComponentStyle(
    size: Size,
    color: ColorStyles = ColorStyles(
        light = ColorStyle.Solid(Color.Cyan),
    ),
    backgroundColor: ColorStyles = ColorStyles(
        light = ColorStyle.Solid(Color.Red),
    ),
    paddingValues: PaddingValues = PaddingValues(10.dp),
    marginValues: PaddingValues = PaddingValues(10.dp),
    border: BorderStyles? = BorderStyles(
        width = 2.dp,
        colors = ColorStyles(light = ColorStyle.Solid(Color.Cyan)),
    ),
    shadow: ShadowStyles? = ShadowStyles(
        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
        radius = 10.dp,
        x = 0.dp,
        y = 3.dp,
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

@Composable
@JvmSynthetic
internal fun previewImageLoader(
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
