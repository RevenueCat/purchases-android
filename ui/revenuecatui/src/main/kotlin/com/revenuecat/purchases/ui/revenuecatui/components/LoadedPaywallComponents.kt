@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.END
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment.BOTTOM
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL

@Composable
internal fun LoadedPaywallComponents(
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    state.update(localeList = configuration.locales)

    val style = state.stack
    val footerComponentStyle = state.stickyFooter
    val background = state.background.toBackgroundStyle()

    Column(modifier = modifier.background(background)) {
        ComponentView(
            style = style,
            state = state,
            onClick = clickHandler,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        )
        footerComponentStyle?.let {
            ComponentView(
                style = it,
                state = state,
                onClick = clickHandler,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    }
}

@Suppress("LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun LoadedPaywallComponents_Preview() {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        TextComponent(
                            text = LocalizationKey("hello-world"),
                            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                        ),
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                ),
                background = Background.Color(
                    ColorScheme(
                        light = ColorInfo.Hex(Color.Blue.toArgb()),
                        dark = ColorInfo.Hex(Color.Red.toArgb()),
                    ),
                ),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("sticky-footer"),
                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                            ),
                        ),
                        dimension = Vertical(alignment = CENTER, distribution = START),
                        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                        shape = Shape.Rectangle(
                            corners = CornerRadiuses(
                                topLeading = 10.0,
                                topTrailing = 10.0,
                                bottomLeading = 0.0,
                                bottomTrailing = 0.0,
                            ),
                        ),
                        shadow = Shadow(
                            ColorScheme(
                                light = ColorInfo.Hex(Color.Black.toArgb()),
                                dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                            ),
                            radius = 10.0,
                            x = 0.0,
                            y = -5.0,
                        ),
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("hello-world") to LocalizationData.Text("Hello, world!"),
                LocalizationKey("sticky-footer") to LocalizationData.Text("Sticky Footer"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "id",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(validated, storefrontCountryCode = null)
    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
        modifier = Modifier
            .fillMaxSize(),
    )
}

@Suppress("LongMethod")
@Preview
@Composable
private fun LoadedPaywallComponents_Preview_Bless() {
    val textColor = ColorScheme(
        light = ColorInfo.Hex(Color.Black.toArgb()),
        dark = ColorInfo.Hex(Color.White.toArgb()),
    )
    val backgroundColor = ColorScheme(
        light = ColorInfo.Hex(Color.White.toArgb()),
        dark = ColorInfo.Hex(Color.Black.toArgb()),
    )
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        StackComponent(
                            components = emptyList(),
                            dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                            size = Size(width = Fill, height = Fill),
                            backgroundColor = ColorScheme(
                                light = ColorInfo.Gradient.Linear(
                                    degrees = 60f,
                                    points = listOf(
                                        ColorInfo.Gradient.Point(
                                            color = Color(red = 0xFF, green = 0xFF, blue = 0xFF, alpha = 0xFF)
                                                .toArgb(),
                                            percent = 40f,
                                        ),
                                        ColorInfo.Gradient.Point(
                                            color = Color(red = 5, green = 124, blue = 91).toArgb(),
                                            percent = 100f,
                                        ),
                                    ),
                                ),
                            ),
                        ),
                        StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("title"),
                                    color = textColor,
                                    fontWeight = FontWeight.SEMI_BOLD,
                                    fontSize = FontSize.HEADING_L,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 0.0, bottom = 40.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("feature-1"),
                                    color = textColor,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("feature-2"),
                                    color = textColor,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("feature-3"),
                                    color = textColor,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("feature-4"),
                                    color = textColor,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("feature-5"),
                                    color = textColor,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("feature-6"),
                                    color = textColor,
                                    horizontalAlignment = LEADING,
                                    size = Size(width = Fill, height = Fit),
                                    margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                ),
                                TextComponent(
                                    text = LocalizationKey("offer"),
                                    color = textColor,
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
                                ),
                            ),
                            dimension = Vertical(alignment = LEADING, distribution = END),
                            size = Size(width = Fill, height = Fill),
                            padding = Padding(top = 16.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
                        ),
                    ),
                    dimension = ZLayer(alignment = BOTTOM),
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
    )
    val offering = Offering(
        identifier = "id",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(validated, storefrontCountryCode = null)

    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
        modifier = Modifier
            .fillMaxSize(),
    )
}
