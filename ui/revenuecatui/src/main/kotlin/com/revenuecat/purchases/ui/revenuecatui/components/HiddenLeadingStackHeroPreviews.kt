@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
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
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import java.net.URL
import java.util.Date

@Preview(name = "PWENG-317 - phone control", device = "spec:width=411dp,height=891dp", showSystemUi = true)
@Composable
private fun HiddenLeadingStackHero_PhoneControl() = HiddenLeadingStackHeroPreview(includeSide = false)

@Preview(name = "PWENG-317 - phone hidden leading side", device = "spec:width=411dp,height=891dp", showSystemUi = true)
@Preview(name = "PWENG-317 - wide visible leading side", device = "spec:width=1000dp,height=891dp", showSystemUi = true)
@Composable
private fun HiddenLeadingStackHero_ResponsiveSide() = HiddenLeadingStackHeroPreview(includeSide = true)

/** Emerge discovers these previews, keeping the hidden-leading-stack regression in the snapshot job. */
@Composable
internal fun HiddenLeadingStackHeroPreview(includeSide: Boolean) {
    HiddenLeadingStackHeroPreview(previewHiddenLeadingStackHeroState(includeSide))
}

/** showSystemUi is not supported by Emerge, so the fixture supplies a real nonzero window inset. */
@Composable
internal fun HiddenLeadingStackHeroPreview(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val view = LocalView.current
    val topInsetPx = with(LocalDensity.current) { 60.dp.roundToPx() }
    WindowInsets.safeDrawing
    DisposableEffect(view, topInsetPx) {
        val previous = ViewCompat.getRootWindowInsets(view) ?: WindowInsetsCompat.Builder().build()
        val insets = WindowInsetsCompat.Builder(previous)
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, topInsetPx, 0, 0))
            .setVisible(WindowInsetsCompat.Type.statusBars(), true)
            .build()
        ViewCompat.dispatchApplyWindowInsets(view, insets)
        onDispose { ViewCompat.dispatchApplyWindowInsets(view, previous) }
    }
    ProvidePreviewImageLoader(previewImageLoader(R.drawable.default_background)) {
        LoadedPaywallComponents(state = state, clickHandler = { }, modifier = modifier)
    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
internal fun previewHiddenLeadingStackHeroState(includeSide: Boolean): PaywallState.Loaded.Components {
    val ink = ColorScheme(ColorInfo.Hex(Color(0xFF123D32).toArgb()))
    val white = ColorScheme(ColorInfo.Hex(Color.White.toArgb()))
    val photoUrl = URL("https://assets.pawwalls.com/hidden-leading-hero-preview.webp")
    val photo = ThemeImageUrls(
        light = ImageUrls(
            original = photoUrl,
            webp = photoUrl,
            webpLowRes = photoUrl,
            width = 462u,
            height = 1000u,
        ),
    )
    fun text(key: String, fontSize: Int, color: ColorScheme = ink) = TextComponent(
        text = LocalizationKey(key),
        color = color,
        fontSize = fontSize,
        size = Size(SizeConstraint.Fill(), SizeConstraint.Fit()),
        horizontalAlignment = HorizontalAlignment.LEADING,
    )
    val wideCondition = listOf(
        ComponentOverride.Condition.WindowWidthRule(
            operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
            value = 800.0,
        ),
    )
    val hero = StackComponent(
        components = listOf(
            ImageComponent(
                source = photo,
                fitMode = FitMode.FILL,
                size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(320u)),
            ),
            TextComponent(
                text = LocalizationKey("brand"),
                color = white,
                fontSize = 18,
                fontWeight = FontWeight.BOLD,
                margin = Padding(top = 16.0, leading = 24.0, trailing = 24.0),
                size = Size(SizeConstraint.Fit(), SizeConstraint.Fit()),
            ),
        ),
        dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP_LEADING),
        size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(320u)),
    )
    val content = StackComponent(
        components = listOf(
            text("title", 28),
            text("benefits", 16),
            PackageComponent(
                packageId = TestData.Packages.monthly.identifier,
                isSelectedByDefault = true,
                stack = StackComponent(components = listOf(text("plan", 16))),
            ),
            PurchaseButtonComponent(
                stack = StackComponent(
                    components = listOf(
                        TextComponent(
                            text = LocalizationKey("cta"),
                            color = white,
                            fontSize = 18,
                            fontWeight = FontWeight.BOLD,
                        ),
                    ),
                    padding = Padding(top = 16.0, bottom = 16.0, leading = 24.0, trailing = 24.0),
                    backgroundColor = ink,
                    size = Size(SizeConstraint.Fill(), SizeConstraint.Fit()),
                ),
            ),
        ),
        padding = Padding(top = 24.0, bottom = 24.0, leading = 24.0, trailing = 24.0),
        spacing = 20f,
    )
    val main = StackComponent(
        components = listOf(hero, content),
        size = Size(SizeConstraint.Fill(), SizeConstraint.Fit()),
    )
    val side = StackComponent(
        components = listOf(
            ImageComponent(
                source = photo,
                fitMode = FitMode.FILL,
                size = Size(SizeConstraint.Fixed(320u), SizeConstraint.Fixed(420u)),
            ),
            text("side", 24, white),
        ),
        visible = false,
        overrides = listOf(
            ComponentOverride(
                conditions = wideCondition,
                properties = PartialStackComponent(visible = true),
            ),
        ),
        backgroundColor = ink,
        size = Size(SizeConstraint.Fixed(320u), SizeConstraint.Fill()),
        spacing = 24f,
    )
    val data = PaywallComponentsData(
        id = "pweng_317_hidden_leading_hero",
        templateName = "components",
        assetBaseURL = photoUrl,
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = if (includeSide) listOf(side, main) else listOf(main),
                    dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP_LEADING),
                    overrides = listOf(
                        ComponentOverride(
                            conditions = wideCondition,
                            properties = PartialStackComponent(
                                dimension = Dimension.Horizontal(
                                    VerticalAlignment.TOP,
                                    FlexDistribution.START,
                                ),
                            ),
                        ),
                    ),
                ),
                background = Background.Color(white),
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("brand") to LocalizationData.Text("FIELD NOTES"),
                LocalizationKey("title") to LocalizationData.Text("Find your next escape"),
                LocalizationKey("benefits") to LocalizationData.Text(
                    "Unlimited trail guides, saved places, and offline maps.",
                ),
                LocalizationKey("plan") to LocalizationData.Text("Monthly · {{ product.price }}/month"),
                LocalizationKey("cta") to LocalizationData.Text("Start exploring"),
                LocalizationKey("side") to LocalizationData.Text("A little further from ordinary"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "pweng_317_preview",
        serverDescription = "Hidden leading hero regression",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    return remember(includeSide) {
        offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = "US",
            dateProvider = { Date(MILLIS_2025_01_25) },
            purchases = MockPurchasesType(),
        )
    }
}
