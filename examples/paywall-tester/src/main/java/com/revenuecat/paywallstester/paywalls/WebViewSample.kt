package com.revenuecat.paywallstester.paywalls

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.paywallstester.SampleData
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.END
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import java.net.URL

private const val PLACEHOLDER_WEB_VIEW_URL = "https://example.com"

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongMethod")
internal fun webViewSample(): SampleData.Components {
    val textColor = ColorScheme(
        light = ColorInfo.Hex(Color.Black.toArgb()),
        dark = ColorInfo.Hex(Color.White.toArgb()),
    )
    val backgroundColor = ColorScheme(
        light = ColorInfo.Hex(Color.White.toArgb()),
        dark = ColorInfo.Hex(Color.Black.toArgb()),
    )
    val accentColor = ColorScheme(
        light = ColorInfo.Hex(Color(red = 5, green = 124, blue = 91).toArgb()),
    )

    return SampleData.Components(
        data = PaywallComponentsData(
            id = "sample_web_view_paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("title"),
                                color = textColor,
                                fontWeight = FontWeight.BOLD,
                                fontSize = 24,
                                horizontalAlignment = LEADING,
                                size = Size(width = Fill, height = Fit),
                                margin = Padding(top = 16.0, bottom = 8.0, leading = 16.0, trailing = 16.0),
                            ),
                            WebViewComponent(
                                id = "sample_web_view",
                                url = PLACEHOLDER_WEB_VIEW_URL,
                                size = Size(width = Fill, height = Fixed(400u)),
                                fallback = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("fallback"),
                                            color = textColor,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            padding = Padding(
                                                top = 16.0,
                                                bottom = 16.0,
                                                leading = 16.0,
                                                trailing = 16.0,
                                            ),
                                        ),
                                    ),
                                    dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                                    size = Size(width = Fill, height = Fixed(400u)),
                                ),
                            ),
                            ButtonComponent(
                                action = ButtonComponent.Action.RestorePurchases,
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("cta"),
                                            color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                                            fontWeight = FontWeight.BOLD,
                                        ),
                                    ),
                                    dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                                    size = Size(width = Fit, height = Fit),
                                    backgroundColor = accentColor,
                                    padding = Padding(
                                        top = 8.0,
                                        bottom = 8.0,
                                        leading = 32.0,
                                        trailing = 32.0,
                                    ),
                                    margin = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
                                    shape = Shape.Pill,
                                ),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = END),
                        size = Size(width = Fill, height = Fill),
                        backgroundColor = backgroundColor,
                    ),
                    background = Background.Color(backgroundColor),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    LocalizationKey("title") to LocalizationData.Text("Web view placeholder"),
                    LocalizationKey("fallback") to LocalizationData.Text("Could not load the web view."),
                    LocalizationKey("cta") to LocalizationData.Text("Restore purchases"),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        ),
    )
}
