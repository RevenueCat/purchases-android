@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
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
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Horizontal
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment.TOP_TRAILING
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import java.net.URL
import java.util.Date

@Suppress("LongMethod", "MagicNumber")
@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Header_ZLayer_Preview() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
    val whiteColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))

    val imageUrl = URL("https://assets.pawwalls.com/1172568_1774614615_69db5d9d.webp")

    val closeButtonIcon = IconComponent(
        baseUrl = "https://icons.pawwalls.com/icons",
        iconName = "x",
        formats = IconComponent.Formats(webp = "x.webp"),
        size = Size(width = Fixed(32u), height = Fixed(32u)),
        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        padding = Padding(top = 6.0, bottom = 6.0, leading = 6.0, trailing = 6.0),
        iconBackground = IconComponent.IconBackground(
            shape = MaskShape.Circle,
            color = whiteColor,
        ),
    )

    val closeButton = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(
            components = listOf(closeButtonIcon),
            dimension = Vertical(alignment = LEADING, distribution = START),
            size = Size(width = Fit, height = Fit),
            margin = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
        ),
    )

    // ZLayer in the body: image with green text overlaid
    val imageWithTextOverlay = StackComponent(
        components = listOf(
            ImageComponent(
                source = ThemeImageUrls(
                    light = ImageUrls(
                        original = imageUrl,
                        webp = imageUrl,
                        webpLowRes = imageUrl,
                        width = 1024u,
                        height = 1024u,
                    ),
                ),
                fitMode = FitMode.FIT,
                size = Size(width = Fill, height = Fit),
            ),
            TextComponent(
                text = LocalizationKey("overlay-text"),
                color = ColorScheme(light = ColorInfo.Hex(Color(0xFF62FC03).toArgb())),
                fontSize = 14,
                horizontalAlignment = LEADING,
                size = Size(width = Fit, height = Fit),
            ),
        ),
        dimension = ZLayer(alignment = TwoDimensionalAlignment.TOP_LEADING),
        size = Size(width = Fill, height = Fit),
    )

    val data = PaywallComponentsData(
        id = "preview_header_zlayer",
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        imageWithTextOverlay,
                        TextComponent(
                            text = LocalizationKey("title"),
                            color = textColor,
                            fontWeight = FontWeight.BOLD,
                            fontSize = 24,
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 0.0, bottom = 0.0, leading = 50.0, trailing = 50.0),
                        ),
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    spacing = 8f,
                ),
                background = Background.Color(whiteColor),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            // ZLayer with just the close button at top-trailing
                            StackComponent(
                                components = listOf(closeButton),
                                dimension = ZLayer(alignment = TOP_TRAILING),
                                size = Size(width = Fill, height = Fit),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fit),
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x33000000).toArgb())),
                            radius = 16.0,
                            x = 0.0,
                            y = 4.0,
                        ),
                    ),
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
                        backgroundColor = whiteColor,
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("overlay-text") to LocalizationData.Text("Featured"),
                LocalizationKey("footer") to LocalizationData.Text("Try free for 1 week"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "header_zlayer",
        serverDescription = "Header with ZLayer image + text",
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

    ProvidePreviewImageLoader(previewImageLoader()) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Preview_HeaderDirectHeroImage() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
    val whiteColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))
    val subtleTextColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF555555).toArgb()))

    val imageUrl = URL("https://assets.pawwalls.com/1172568_1774614837_7df8aa27.webp")
    val heroImage = ImageComponent(
        source = ThemeImageUrls(
            light = ImageUrls(
                original = imageUrl,
                webp = imageUrl,
                webpLowRes = imageUrl,
                width = 1024u,
                height = 1024u,
            ),
        ),
        fitMode = FitMode.FIT,
        size = Size(width = Fill, height = Fit),
        colorOverlay = ColorScheme(light = ColorInfo.Hex(Color(0x33000000).toArgb())),
    )

    val closeButtonIcon = IconComponent(
        baseUrl = "https://icons.pawwalls.com/icons",
        iconName = "x",
        formats = IconComponent.Formats(webp = "x.webp"),
        size = Size(width = Fixed(32u), height = Fixed(32u)),
        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        padding = Padding(top = 6.0, bottom = 6.0, leading = 6.0, trailing = 6.0),
        iconBackground = IconComponent.IconBackground(
            shape = MaskShape.Circle,
            color = whiteColor,
        ),
    )

    val closeButton = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(
            components = listOf(closeButtonIcon),
            dimension = Vertical(alignment = LEADING, distribution = START),
            size = Size(width = Fit, height = Fit),
            margin = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
        ),
    )

    val data = PaywallComponentsData(
        id = "preview_header_direct_hero",
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
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 16.0, bottom = 0.0, leading = 50.0, trailing = 50.0),
                        ),
                        TextComponent(
                            text = LocalizationKey("subtitle"),
                            color = textColor,
                            fontSize = 15,
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 8.0, bottom = 0.0, leading = 24.0, trailing = 24.0),
                        ),
                        TestData.Components.monthlyPackageComponent,
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    spacing = 8f,
                ),
                background = Background.Color(whiteColor),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            heroImage,
                            closeButton,
                        ),
                        dimension = ZLayer(alignment = TOP_TRAILING),
                        size = Size(width = Fill, height = Fit),
                        border = Border(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x44000000).toArgb())),
                            width = 1.0,
                        ),
                    ),
                ),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            PackageComponent(
                                packageId = "\$rc_annual",
                                isSelectedByDefault = true,
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("package-info"),
                                            color = textColor,
                                            fontSize = 14,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fit, height = Fit),
                                        ),
                                    ),
                                    dimension = Vertical(
                                        alignment = CENTER,
                                        distribution = START,
                                    ),
                                    size = Size(width = Fill, height = Fit),
                                    shape = Shape.Rectangle(
                                        corners = CornerRadiuses.Dp(all = 8.0),
                                    ),
                                ),
                            ),
                            PurchaseButtonComponent(
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("cta"),
                                            color = ColorScheme(
                                                light = ColorInfo.Hex(Color(0xFF0C0C0C).toArgb()),
                                            ),
                                            fontWeight = FontWeight.SEMI_BOLD,
                                            fontSize = 16,
                                            horizontalAlignment = CENTER,
                                            size = Size(width = Fit, height = Fit),
                                        ),
                                    ),
                                    dimension = Vertical(
                                        alignment = CENTER,
                                        distribution = START,
                                    ),
                                    size = Size(width = Fill, height = Fit),
                                    padding = Padding(
                                        top = 12.0,
                                        bottom = 12.0,
                                        leading = 8.0,
                                        trailing = 8.0,
                                    ),
                                    backgroundColor = ColorScheme(
                                        light = ColorInfo.Gradient.Linear(
                                            degrees = 15f,
                                            points = listOf(
                                                ColorInfo.Gradient.Point(
                                                    color = Color(0xFF7CECA7).toArgb(),
                                                    percent = 0f,
                                                ),
                                                ColorInfo.Gradient.Point(
                                                    color = Color(0xFF7EF0E3).toArgb(),
                                                    percent = 100f,
                                                ),
                                            ),
                                        ),
                                    ),
                                    shape = Shape.Rectangle(
                                        corners = CornerRadiuses.Dp(all = 12.0),
                                    ),
                                    shadow = Shadow(
                                        color = ColorScheme(
                                            light = ColorInfo.Hex(Color(0x808CFFBC).toArgb()),
                                        ),
                                        radius = 8.0,
                                        x = 4.0,
                                        y = 4.0,
                                    ),
                                ),
                            ),
                            StackComponent(
                                components = listOf(
                                    ButtonComponent(
                                        action = ButtonComponent.Action.RestorePurchases,
                                        stack = StackComponent(
                                            components = listOf(
                                                TextComponent(
                                                    text = LocalizationKey("restore"),
                                                    color = subtleTextColor,
                                                    fontWeight = FontWeight.SEMI_BOLD,
                                                    fontSize = 13,
                                                    horizontalAlignment = LEADING,
                                                    size = Size(width = Fit, height = Fit),
                                                ),
                                            ),
                                            dimension = Vertical(
                                                alignment = LEADING,
                                                distribution = FlexDistribution.SPACE_BETWEEN,
                                            ),
                                            size = Size(width = Fit, height = Fit),
                                        ),
                                    ),
                                ),
                                dimension = Horizontal(
                                    alignment = VerticalAlignment.TOP,
                                    distribution = FlexDistribution.CENTER,
                                ),
                                size = Size(width = Fill, height = Fit),
                                margin = Padding(top = 12.0, bottom = 0.0, leading = 0.0, trailing = 0.0),
                                spacing = 32f,
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fit),
                        padding = Padding(top = 12.0, bottom = 0.0, leading = 16.0, trailing = 16.0),
                        backgroundColor = whiteColor,
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x0F000000).toArgb())),
                            radius = 16.0,
                            x = 0.0,
                            y = -4.0,
                        ),
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("subtitle") to LocalizationData.Text(
                    "AI-powered flashcards, smart scheduling, and progress tracking.",
                ),
                LocalizationKey("package-info") to LocalizationData.Text(
                    "Subscribe to Pro for just {{ product.price_per_period_abbreviated }}",
                ),
                LocalizationKey("cta") to LocalizationData.Text("Continue"),
                LocalizationKey("restore") to LocalizationData.Text("Restore Purchases"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "header_direct_hero",
        serverDescription = "Header with direct hero image",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.annual),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = "US",
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    ProvidePreviewImageLoader(previewImageLoader()) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Preview_HeaderTextOnly() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
    val whiteColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))
    val subtleTextColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF4B4949).toArgb()))

    val closeButtonIcon = IconComponent(
        baseUrl = "https://icons.pawwalls.com/icons",
        iconName = "x",
        formats = IconComponent.Formats(webp = "x.webp"),
        size = Size(width = Fixed(32u), height = Fixed(32u)),
        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        padding = Padding(top = 6.0, bottom = 6.0, leading = 6.0, trailing = 6.0),
        iconBackground = IconComponent.IconBackground(
            shape = MaskShape.Circle,
            color = whiteColor,
        ),
    )

    val closeButton = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(
            components = listOf(closeButtonIcon),
            dimension = Vertical(alignment = LEADING, distribution = START),
            size = Size(width = Fit, height = Fit),
            margin = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
        ),
    )

    // Feature list items
    val featureTexts = listOf("feature-1", "feature-2").map { key ->
        TextComponent(
            text = LocalizationKey(key),
            color = subtleTextColor,
            fontSize = 14,
            horizontalAlignment = LEADING,
            size = Size(width = Fill, height = Fit),
            padding = Padding(top = 2.0, bottom = 2.0, leading = 0.0, trailing = 0.0),
        )
    }

    val data = PaywallComponentsData(
        id = "preview_header_text_only",
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
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 0.0, bottom = 0.0, leading = 50.0, trailing = 50.0),
                        ),
                        // Features section
                        StackComponent(
                            components = listOf(
                                StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("features-header"),
                                            color = whiteColor,
                                            fontWeight = FontWeight.BOLD,
                                            fontSize = 16,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fit, height = Fit),
                                        ),
                                    ) + featureTexts,
                                    dimension = Vertical(alignment = LEADING, distribution = START),
                                    size = Size(width = Fill, height = Fit),
                                    spacing = 4f,
                                ),
                            ),
                            dimension = Vertical(alignment = LEADING, distribution = START),
                            size = Size(width = Fill, height = Fit),
                            padding = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
                        ),
                        TestData.Components.monthlyPackageComponent,
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    spacing = 8f,
                ),
                background = Background.Color(whiteColor),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(closeButton),
                                dimension = ZLayer(alignment = TOP_TRAILING),
                                size = Size(width = Fill, height = Fit),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fit),
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x33000000).toArgb())),
                            radius = 16.0,
                            x = 0.0,
                            y = 4.0,
                        ),
                    ),
                ),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            PurchaseButtonComponent(
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("cta"),
                                            color = ColorScheme(
                                                light = ColorInfo.Hex(Color(0xFF0C0C0C).toArgb()),
                                            ),
                                            fontWeight = FontWeight.SEMI_BOLD,
                                            fontSize = 16,
                                            horizontalAlignment = CENTER,
                                            size = Size(width = Fit, height = Fit),
                                        ),
                                    ),
                                    dimension = Vertical(
                                        alignment = CENTER,
                                        distribution = START,
                                    ),
                                    size = Size(width = Fill, height = Fit),
                                    padding = Padding(
                                        top = 12.0,
                                        bottom = 12.0,
                                        leading = 8.0,
                                        trailing = 8.0,
                                    ),
                                    backgroundColor = ColorScheme(
                                        light = ColorInfo.Gradient.Linear(
                                            degrees = 15f,
                                            points = listOf(
                                                ColorInfo.Gradient.Point(
                                                    color = Color(0xFF7CECA7).toArgb(),
                                                    percent = 0f,
                                                ),
                                                ColorInfo.Gradient.Point(
                                                    color = Color(0xFF7EF0E3).toArgb(),
                                                    percent = 100f,
                                                ),
                                            ),
                                        ),
                                    ),
                                    shape = Shape.Rectangle(
                                        corners = CornerRadiuses.Dp(all = 12.0),
                                    ),
                                ),
                            ),
                            StackComponent(
                                components = listOf(
                                    ButtonComponent(
                                        action = ButtonComponent.Action.RestorePurchases,
                                        stack = StackComponent(
                                            components = listOf(
                                                TextComponent(
                                                    text = LocalizationKey("restore"),
                                                    color = subtleTextColor,
                                                    fontWeight = FontWeight.SEMI_BOLD,
                                                    fontSize = 13,
                                                    horizontalAlignment = LEADING,
                                                    size = Size(width = Fit, height = Fit),
                                                ),
                                            ),
                                            dimension = Vertical(
                                                alignment = LEADING,
                                                distribution = FlexDistribution.SPACE_BETWEEN,
                                            ),
                                            size = Size(width = Fit, height = Fit),
                                        ),
                                    ),
                                ),
                                dimension = Horizontal(
                                    alignment = VerticalAlignment.TOP,
                                    distribution = FlexDistribution.CENTER,
                                ),
                                size = Size(width = Fill, height = Fit),
                                margin = Padding(top = 12.0, bottom = 0.0, leading = 0.0, trailing = 0.0),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fit),
                        padding = Padding(top = 12.0, bottom = 0.0, leading = 16.0, trailing = 16.0),
                        backgroundColor = whiteColor,
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x0F000000).toArgb())),
                            radius = 16.0,
                            x = 0.0,
                            y = -4.0,
                        ),
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("features-header") to LocalizationData.Text("Features"),
                LocalizationKey("feature-1") to LocalizationData.Text("Assignment Tracker"),
                LocalizationKey("feature-2") to LocalizationData.Text("AI-generated Study Plans"),
                LocalizationKey("cta") to LocalizationData.Text("Continue"),
                LocalizationKey("restore") to LocalizationData.Text("Restore Purchases"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "header_text_only",
        serverDescription = "Header with text only (no hero image)",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.annual),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = "US",
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    ProvidePreviewImageLoader(previewImageLoader()) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Preview_HeaderNoZStackNoImage() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
    val whiteColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))

    val closeButtonIcon = IconComponent(
        baseUrl = "https://icons.pawwalls.com/icons",
        iconName = "x",
        formats = IconComponent.Formats(webp = "x.webp"),
        size = Size(width = Fixed(32u), height = Fixed(32u)),
        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        padding = Padding(top = 6.0, bottom = 6.0, leading = 6.0, trailing = 6.0),
        iconBackground = IconComponent.IconBackground(
            shape = MaskShape.Circle,
            color = whiteColor,
        ),
    )

    val closeButton = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(
            components = listOf(closeButtonIcon),
            dimension = Vertical(alignment = LEADING, distribution = START),
            size = Size(width = Fit, height = Fit),
            margin = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
        ),
    )

    val data = PaywallComponentsData(
        id = "preview_header_no_zstack_no_image",
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
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 0.0, bottom = 0.0, leading = 50.0, trailing = 50.0),
                        ),
                        TestData.Components.monthlyPackageComponent,
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    spacing = 8f,
                ),
                background = Background.Color(whiteColor),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(closeButton),
                                dimension = ZLayer(alignment = TOP_TRAILING),
                                size = Size(width = Fill, height = Fit),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fit),
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x33000000).toArgb())),
                            radius = 16.0,
                            x = 0.0,
                            y = 4.0,
                        ),
                    ),
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
                        backgroundColor = whiteColor,
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("footer") to LocalizationData.Text("Try free for 1 week"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "header_no_zstack_no_image",
        serverDescription = "Header with no ZStack and no image",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.annual),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = "US",
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    ProvidePreviewImageLoader(previewImageLoader()) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Preview_DirectImageAsBackground() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
    val whiteColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))

    val imageUrl = URL("https://assets.pawwalls.com/1172568_1774614837_7df8aa27.webp")

    // ZLayer stack with background image and text overlay — no header component
    val imageBackgroundStack = StackComponent(
        components = listOf(
            TextComponent(
                text = LocalizationKey("overlay-text"),
                color = ColorScheme(light = ColorInfo.Hex(Color(0xFFF90101).toArgb())),
                fontSize = 14,
                horizontalAlignment = LEADING,
                size = Size(width = Fit, height = Fit),
            ),
        ),
        dimension = ZLayer(alignment = TwoDimensionalAlignment.TOP_LEADING),
        size = Size(width = Fill, height = Fixed(300u)),
        background = Background.Image(
            value = ThemeImageUrls(
                light = ImageUrls(
                    original = imageUrl,
                    webp = imageUrl,
                    webpLowRes = imageUrl,
                    width = 1024u,
                    height = 1024u,
                ),
            ),
            fitMode = FitMode.FILL,
        ),
    )

    val data = PaywallComponentsData(
        id = "preview_direct_image_background",
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        imageBackgroundStack,
                        TextComponent(
                            text = LocalizationKey("title"),
                            color = textColor,
                            fontWeight = FontWeight.BOLD,
                            fontSize = 24,
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 0.0, bottom = 0.0, leading = 50.0, trailing = 50.0),
                        ),
                        TestData.Components.monthlyPackageComponent,
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    spacing = 8f,
                ),
                background = Background.Color(whiteColor),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("footer"),
                                color = textColor,
                            ),
                        ),
                        dimension = Vertical(alignment = CENTER, distribution = START),
                        backgroundColor = whiteColor,
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("overlay-text") to LocalizationData.Text("Text in stack"),
                LocalizationKey("footer") to LocalizationData.Text("Try free for 1 week"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "direct_image_background",
        serverDescription = "Direct image as background, no header",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.annual),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = "US",
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    ProvidePreviewImageLoader(previewImageLoader()) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
@Preview(showSystemUi = true)
@Composable
private fun LoadedPaywallComponents_Preview_HeaderNestedStackWithImage() {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
    val whiteColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))

    val imageUrl = URL("https://assets.pawwalls.com/1172568_1774614837_7df8aa27.webp")

    val closeButtonIcon = IconComponent(
        baseUrl = "https://icons.pawwalls.com/icons",
        iconName = "x",
        formats = IconComponent.Formats(webp = "x.webp"),
        size = Size(width = Fixed(32u), height = Fixed(32u)),
        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        padding = Padding(top = 6.0, bottom = 6.0, leading = 6.0, trailing = 6.0),
        iconBackground = IconComponent.IconBackground(
            shape = MaskShape.Circle,
            color = whiteColor,
        ),
    )

    val closeButton = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(
            components = listOf(closeButtonIcon),
            dimension = Vertical(alignment = LEADING, distribution = START),
            size = Size(width = Fit, height = Fit),
            margin = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
        ),
    )

    // Nested ZLayer: image + text overlay inside a wrapper stack
    val nestedImageZLayer = StackComponent(
        components = listOf(
            StackComponent(
                components = listOf(
                    ImageComponent(
                        source = ThemeImageUrls(
                            light = ImageUrls(
                                original = imageUrl,
                                webp = imageUrl,
                                webpLowRes = imageUrl,
                                width = 1024u,
                                height = 1024u,
                            ),
                        ),
                        fitMode = FitMode.FIT,
                        size = Size(width = Fill, height = Fit),
                    ),
                    TextComponent(
                        text = LocalizationKey("overlay-text"),
                        color = ColorScheme(light = ColorInfo.Hex(Color(0xFFF90101).toArgb())),
                        fontSize = 14,
                        horizontalAlignment = LEADING,
                        size = Size(width = Fit, height = Fit),
                    ),
                ),
                dimension = ZLayer(alignment = TwoDimensionalAlignment.TOP_LEADING),
                size = Size(width = Fill, height = Fit),
            ),
        ),
        dimension = Vertical(alignment = CENTER, distribution = START),
        size = Size(width = Fill, height = Fit),
    )

    val data = PaywallComponentsData(
        id = "preview_header_nested_stack_image",
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        nestedImageZLayer,
                        TextComponent(
                            text = LocalizationKey("title"),
                            color = textColor,
                            fontWeight = FontWeight.BOLD,
                            fontSize = 24,
                            horizontalAlignment = CENTER,
                            size = Size(width = Fit, height = Fit),
                            margin = Padding(top = 0.0, bottom = 0.0, leading = 50.0, trailing = 50.0),
                        ),
                        TestData.Components.monthlyPackageComponent,
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fit),
                    spacing = 8f,
                ),
                background = Background.Color(whiteColor),
                header = HeaderComponent(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(closeButton),
                                dimension = ZLayer(alignment = TOP_TRAILING),
                                size = Size(width = Fill, height = Fit),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fit),
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(Color(0x33000000).toArgb())),
                            radius = 16.0,
                            x = 0.0,
                            y = 4.0,
                        ),
                    ),
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
                        backgroundColor = whiteColor,
                    ),
                ),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock Your Smartest Study Routine"),
                LocalizationKey("overlay-text") to LocalizationData.Text("Text in stack"),
                LocalizationKey("footer") to LocalizationData.Text("Try free for 1 week"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "header_nested_stack_image",
        serverDescription = "Header with nested stack containing image",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.annual),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = "US",
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )

    ProvidePreviewImageLoader(previewImageLoader()) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
