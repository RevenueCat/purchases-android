package com.revenuecat.paywallstester.paywalls

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.paywallstester.SampleData
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Horizontal
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.CENTER
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.END
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import java.net.URL

@Suppress("LongMethod")
@OptIn(InternalRevenueCatAPI::class)
internal fun tabsWithButtons(font: FontAlias? = null): SampleData.Components {
    val textColor = ColorScheme(
        light = ColorInfo.Hex(Color.Black.toArgb()),
        dark = ColorInfo.Hex(Color.White.toArgb()),
    )
    val backgroundColor = ColorScheme(
        light = ColorInfo.Hex(Color.White.toArgb()),
        dark = ColorInfo.Hex(Color.Black.toArgb()),
    )

    val boldWhenSelectedOverride = ComponentOverride(
        conditions = listOf(ComponentOverride.Condition.Selected),
        properties = PartialTextComponent(
            fontWeight = FontWeight.EXTRA_BOLD,
        ),
    )

    val cyanBackgroundWhenSelectedOverride = ComponentOverride(
        conditions = listOf(ComponentOverride.Condition.Selected),
        properties = PartialStackComponent(
            backgroundColor = ColorScheme(
                light = ColorInfo.Hex(Color.Cyan.toArgb()),
            ),
        ),
    )

    return SampleData.Components(
        data = PaywallComponentsData(
            id = "sample_tabs_with_buttons_paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("title"),
                                color = textColor,
                                fontName = font,
                                fontWeight = FontWeight.BOLD,
                                fontSize = 28,
                                horizontalAlignment = LEADING,
                                size = Size(width = Fill, height = Fit),
                                margin = Padding(top = 32.0, bottom = 40.0, leading = 16.0, trailing = 16.0),
                            ),
                            // Tabs
                            TabsComponent(
                                control = TabsComponent.TabControl.Buttons(
                                    stack = StackComponent(
                                        components = (0..2).map { index ->
                                            TabControlButtonComponent(
                                                tabIndex = index,
                                                tabId = "tab-$index",
                                                stack = StackComponent(
                                                    components = listOf(
                                                        TextComponent(
                                                            text = LocalizationKey("tab-$index"),
                                                            color = textColor,
                                                            size = Size(width = Fit, height = Fit),
                                                            fontName = font,
                                                            overrides = listOf(boldWhenSelectedOverride),
                                                            padding = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 16.0,
                                                                trailing = 16.0,
                                                            ),
                                                        ),
                                                    ),
                                                    size = Size(width = Fit, height = Fit),
                                                    overrides = listOf(cyanBackgroundWhenSelectedOverride),
                                                ),
                                            )
                                        },
                                        dimension = Horizontal(alignment = VerticalAlignment.CENTER, CENTER),
                                        size = Size(width = Fit, height = Fit),
                                        shape = Shape.Pill,
                                        backgroundColor = ColorScheme(
                                            light = ColorInfo.Hex(Color.LightGray.toArgb()),
                                        ),
                                    ),
                                ),
                                tabs = listOf(
                                    TabsComponent.Tab(
                                        id = "tab-0",
                                        stack = StackComponent(
                                            components = listOf(
                                                TabControlComponent,
                                                StackComponent(
                                                    components = listOf(
                                                        TextComponent(
                                                            text = LocalizationKey("feature-1"),
                                                            color = textColor,
                                                            fontName = font,
                                                            horizontalAlignment = LEADING,
                                                            size = Size(width = Fill, height = Fit),
                                                            margin = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 0.0,
                                                                trailing = 0.0,
                                                            ),
                                                        ),
                                                        TextComponent(
                                                            text = LocalizationKey("feature-2"),
                                                            color = textColor,
                                                            fontName = font,
                                                            horizontalAlignment = LEADING,
                                                            size = Size(width = Fill, height = Fit),
                                                            margin = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 0.0,
                                                                trailing = 0.0,
                                                            ),
                                                        ),
                                                    ),
                                                    size = Size(width = Fill, height = Fill),
                                                    backgroundColor = ColorScheme(
                                                        light = ColorInfo.Hex(Color.Red.toArgb()),
                                                    ),
                                                    margin = Padding(top = 16.0, leading = 16.0, trailing = 16.0),
                                                    padding = Padding(
                                                        top = 16.0,
                                                        bottom = 16.0,
                                                        leading = 16.0,
                                                        trailing = 16.0,
                                                    ),
                                                    shape = Shape.Rectangle(CornerRadiuses.Dp(all = 16.0)),
                                                ),
                                            ),
                                            size = Size(width = Fill, height = Fill),
                                        ),
                                    ),
                                    TabsComponent.Tab(
                                        id = "tab-1",
                                        stack = StackComponent(
                                            components = listOf(
                                                TabControlComponent,
                                                StackComponent(
                                                    components = listOf(
                                                        TextComponent(
                                                            text = LocalizationKey("feature-3"),
                                                            color = textColor,
                                                            fontName = font,
                                                            horizontalAlignment = LEADING,
                                                            size = Size(width = Fill, height = Fit),
                                                            margin = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 0.0,
                                                                trailing = 0.0,
                                                            ),
                                                        ),
                                                        TextComponent(
                                                            text = LocalizationKey("feature-4"),
                                                            color = textColor,
                                                            fontName = font,
                                                            horizontalAlignment = LEADING,
                                                            size = Size(width = Fill, height = Fit),
                                                            margin = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 0.0,
                                                                trailing = 0.0,
                                                            ),
                                                        ),
                                                    ),
                                                    size = Size(width = Fill, height = Fill),
                                                    backgroundColor = ColorScheme(
                                                        light = ColorInfo.Hex(Color.Yellow.toArgb()),
                                                    ),
                                                    margin = Padding(top = 16.0, leading = 16.0, trailing = 16.0),
                                                    padding = Padding(
                                                        top = 16.0,
                                                        bottom = 16.0,
                                                        leading = 16.0,
                                                        trailing = 16.0,
                                                    ),
                                                    shape = Shape.Rectangle(CornerRadiuses.Dp(all = 16.0)),
                                                ),
                                            ),
                                            size = Size(width = Fill, height = Fill),
                                        ),
                                    ),
                                    TabsComponent.Tab(
                                        id = "tab-2",
                                        stack = StackComponent(
                                            components = listOf(
                                                TabControlComponent,
                                                StackComponent(
                                                    components = listOf(
                                                        TextComponent(
                                                            text = LocalizationKey("feature-5"),
                                                            color = textColor,
                                                            fontName = font,
                                                            horizontalAlignment = LEADING,
                                                            size = Size(width = Fill, height = Fit),
                                                            margin = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 0.0,
                                                                trailing = 0.0,
                                                            ),
                                                        ),
                                                        TextComponent(
                                                            text = LocalizationKey("feature-6"),
                                                            color = textColor,
                                                            fontName = font,
                                                            horizontalAlignment = LEADING,
                                                            size = Size(width = Fill, height = Fit),
                                                            margin = Padding(
                                                                top = 8.0,
                                                                bottom = 8.0,
                                                                leading = 0.0,
                                                                trailing = 0.0,
                                                            ),
                                                        ),
                                                    ),
                                                    size = Size(width = Fill, height = Fill),
                                                    backgroundColor = ColorScheme(
                                                        light = ColorInfo.Hex(Color.Blue.toArgb()),
                                                    ),
                                                    margin = Padding(top = 16.0, leading = 16.0, trailing = 16.0),
                                                    padding = Padding(
                                                        top = 16.0,
                                                        bottom = 16.0,
                                                        leading = 16.0,
                                                        trailing = 16.0,
                                                    ),
                                                    shape = Shape.Rectangle(CornerRadiuses.Dp(all = 16.0)),
                                                ),
                                            ),
                                            size = Size(width = Fill, height = Fill),
                                        ),
                                    ),
                                ),
                                size = Size(width = Fill, height = Fill),
                            ),

                            StackComponent(
                                components = listOf(
                                    TextComponent(
                                        text = LocalizationKey("offer"),
                                        color = textColor,
                                        fontName = font,
                                        horizontalAlignment = LEADING,
                                        size = Size(width = Fill, height = Fit),
                                        margin = Padding(top = 48.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                    ),
                                    StackComponent(
                                        components = listOf(
                                            TextComponent(
                                                text = LocalizationKey("cta"),
                                                color = ColorScheme(
                                                    light = ColorInfo.Hex(Color.White.toArgb()),
                                                ),
                                                fontName = font,
                                                fontWeight = FontWeight.BOLD,
                                            ),
                                        ),
                                        dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                                        size = Size(width = Fit, height = Fit),
                                        backgroundColor = ColorScheme(
                                            light = ColorInfo.Hex(Color(red = 5, green = 124, blue = 91).toArgb()),
                                        ),
                                        padding = Padding(top = 8.0, bottom = 8.0, leading = 32.0, trailing = 32.0),
                                        margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        shape = Shape.Pill,
                                    ),
                                    TextComponent(
                                        text = LocalizationKey("terms"),
                                        color = textColor,
                                        fontName = font,
                                    ),
                                ),
                                dimension = Vertical(alignment = LEADING, distribution = END),
                                size = Size(width = Fill, height = Fit),
                                padding = Padding(top = 16.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = CENTER),
                        size = Size(width = Fill, height = Fill),
                        backgroundColor = backgroundColor,
                    ),
                    background = Background.Color(backgroundColor),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    LocalizationKey("title") to LocalizationData.Text("Unlock bless."),
                    LocalizationKey("tab-0") to LocalizationData.Text("Tab 1"),
                    LocalizationKey("tab-1") to LocalizationData.Text("Tab 2"),
                    LocalizationKey("tab-2") to LocalizationData.Text("Tab 3"),
                    LocalizationKey("feature-1") to LocalizationData.Text("✓ Enjoy a 7 day trial"),
                    LocalizationKey("feature-2") to LocalizationData.Text("✓ Change currencies"),
                    LocalizationKey("feature-3") to LocalizationData.Text("✓ Access more trend charts"),
                    LocalizationKey("feature-4") to LocalizationData.Text("✓ Create custom categories"),
                    LocalizationKey("feature-5") to LocalizationData.Text("✓ Get a special premium icon"),
                    LocalizationKey("feature-6") to LocalizationData.Text(
                        "✓ Receive our love and gratitude for your support",
                    ),
                    LocalizationKey("offer") to LocalizationData.Text(
                        "Try 7 days free, then $19.98/year. Cancel anytime.",
                    ),
                    LocalizationKey("cta") to LocalizationData.Text("Continue"),
                    LocalizationKey("terms") to LocalizationData.Text("Privacy & Terms"),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        ),
    )
}
