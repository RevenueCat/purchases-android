@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
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
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import java.net.URL
import java.util.Date

@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Header_ZLayer_Preview() {
    val headerBackground = ColorScheme(light = ColorInfo.Hex(0xFFBF5F5F.toInt()))
    val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
    val data = PaywallComponentsData(
        id = "preview_paywall_id",
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
                            size = Size(width = Fill, height = Fit),
                        ),
                        TextComponent(
                            text = LocalizationKey("subtitle"),
                            color = textColor,
                            size = Size(width = Fill, height = Fit),
                        ),
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    padding = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
                ),
                background = Background.Color(
                    ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                ),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("footer"),
                                color = textColor,
                            ),
                        ),
                        dimension = Vertical(alignment = CENTER, distribution = START),
                        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                    ),
                ),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("header-text"),
                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                fontSize = 14,
                            ),
                        ),
                        dimension = ZLayer(alignment = TwoDimensionalAlignment.LEADING),
                        size = Size(width = Fill, height = Fit),
                        backgroundColor = headerBackground,
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("subtitle") to LocalizationData.Text("Get access to all features"),
                LocalizationKey("footer") to LocalizationData.Text("Try free for 1 week"),
                LocalizationKey("header-text") to LocalizationData.Text("Text in header"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "id",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = null,
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
        modifier = Modifier
            .fillMaxSize(),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Header_HeroImage_Preview() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
    val data = PaywallComponentsData(
        id = "preview_paywall_id",
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        StackComponent(
                            components = listOf(
                                ImageComponent(
                                    source = ThemeImageUrls(
                                        light = ImageUrls(
                                            original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                            webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                            webpLowRes = URL(
                                                "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                                            ),
                                            width = 547.toUInt(),
                                            height = 257.toUInt(),
                                        ),
                                    ),
                                    size = Size(width = Fill, height = Fit),
                                ),
                                TextComponent(
                                    text = LocalizationKey("title"),
                                    color = textColor,
                                    fontWeight = FontWeight.BOLD,
                                    fontSize = 24,
                                    size = Size(width = Fill, height = Fit),
                                ),
                            ),
                            dimension = ZLayer(alignment = TwoDimensionalAlignment.TOP),
                            size = Size(width = Fill, height = Fit),
                        ),
                        TextComponent(
                            text = LocalizationKey("subtitle"),
                            color = textColor,
                            size = Size(width = Fill, height = Fit),
                        ),
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                ),
                background = Background.Color(
                    ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                ),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("footer"),
                                color = textColor,
                            ),
                        ),
                        dimension = Vertical(alignment = CENTER, distribution = START),
                        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                    ),
                ),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("header-text"),
                                color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                                fontSize = 14,
                            ),
                        ),
                        dimension = ZLayer(alignment = TwoDimensionalAlignment.LEADING),
                        size = Size(width = Fill, height = Fit),
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("subtitle") to LocalizationData.Text("Get access to all features"),
                LocalizationKey("footer") to LocalizationData.Text("Try free for 1 week"),
                LocalizationKey("header-text") to LocalizationData.Text("Text in header"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "id",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = null,
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
        modifier = Modifier
            .fillMaxSize(),
    )
}
