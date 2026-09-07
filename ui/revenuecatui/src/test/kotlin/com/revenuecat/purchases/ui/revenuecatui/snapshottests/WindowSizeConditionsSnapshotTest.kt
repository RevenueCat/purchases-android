package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
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
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Horizontal
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.CENTER
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.MILLIS_2025_01_25
import com.revenuecat.purchases.ui.revenuecatui.components.previewUiConfig
import com.revenuecat.purchases.ui.revenuecatui.components.validatePaywallComponentsDataOrNullForPreviews
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL
import java.util.Date

/**
 * Snapshots for window size conditions: two panes stacked vertically by default, flipped
 * side by side by an override conditioned on `window width >= 700dp AND height >= 480dp`.
 * Across the shared device configs this covers all three outcomes: phone portrait stays
 * stacked (width), phone landscape stays stacked (the height floor), and tablets flip.
 */
@OptIn(InternalRevenueCatAPI::class)
@RunWith(Parameterized::class)
internal class WindowSizeConditionsSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {

    @Test
    fun splitLayoutFollowsWindowSize() {
        screenshotTest { WindowSplitPaywall() }
    }

    @Composable
    private fun WindowSplitPaywall() {
        LoadedPaywallComponents(
            state = buildState(),
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun buildState(): PaywallState.Loaded.Components {
        val data = PaywallComponentsData(
            id = "snapshot_window_size_conditions",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = splitWrapper(),
                    background = Background.Color(contentBackgroundColor),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    LocalizationKey("title") to LocalizationData.Text("Experience Pro today!"),
                    LocalizationKey("body") to LocalizationData.Text("Check out the power of all we offer."),
                    LocalizationKey("package") to LocalizationData.Text("Monthly — $9.99/mo"),
                    LocalizationKey("cta") to LocalizationData.Text("Continue"),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "window_size_conditions",
            serverDescription = "Window size conditions",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
        return offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = "US",
            dateProvider = { Date(MILLIS_2025_01_25) },
            purchases = MockPurchasesType(),
        )
    }

    private fun splitWrapper(): StackComponent = StackComponent(
        components = listOf(contentPane(), purchasePane()),
        dimension = Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
        size = Size(width = Fill(), height = Fill()),
        overrides = listOf(
            ComponentOverride(
                conditions = listOf(
                    ComponentOverride.Condition.WindowWidthRule(
                        operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                        value = 700.0,
                    ),
                    ComponentOverride.Condition.WindowHeightRule(
                        operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                        value = 480.0,
                    ),
                ),
                properties = PartialStackComponent(
                    dimension = Horizontal(alignment = VerticalAlignment.TOP, distribution = START),
                ),
            ),
        ),
    )

    private fun contentPane(): StackComponent = StackComponent(
        components = listOf(
            TextComponent(
                text = LocalizationKey("title"),
                color = textColor,
                fontWeight = FontWeight.BOLD,
                fontSize = 28,
                size = Size(width = Fill(), height = Fit()),
                margin = Padding(top = 48.0, bottom = 8.0, leading = 24.0, trailing = 24.0),
            ),
            TextComponent(
                text = LocalizationKey("body"),
                color = textColor,
                size = Size(width = Fill(), height = Fit()),
                margin = Padding(top = 0.0, bottom = 24.0, leading = 24.0, trailing = 24.0),
            ),
        ),
        dimension = Vertical(alignment = HorizontalAlignment.CENTER, distribution = CENTER),
        size = Size(width = Fill(), height = Fill()),
        backgroundColor = contentPaneColor,
    )

    private fun purchasePane(): StackComponent = StackComponent(
        components = listOf(
            TextComponent(
                text = LocalizationKey("package"),
                color = textColor,
                fontWeight = FontWeight.SEMI_BOLD,
                size = Size(width = Fill(), height = Fit()),
                margin = Padding(top = 0.0, bottom = 16.0, leading = 24.0, trailing = 24.0),
            ),
            TextComponent(
                text = LocalizationKey("cta"),
                color = ctaTextColor,
                fontWeight = FontWeight.BOLD,
                backgroundColor = ctaColor,
                size = Size(width = Fill(), height = Fit()),
                padding = Padding(top = 12.0, bottom = 12.0, leading = 24.0, trailing = 24.0),
                margin = Padding(top = 0.0, bottom = 0.0, leading = 24.0, trailing = 24.0),
            ),
        ),
        dimension = Vertical(alignment = HorizontalAlignment.CENTER, distribution = CENTER),
        size = Size(width = Fill(), height = Fill()),
        backgroundColor = purchasePaneColor,
    )

    private companion object {
        val textColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF272727).toArgb()))
        val ctaTextColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))
        val ctaColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFE89D89).toArgb()))
        val contentBackgroundColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()))
        val contentPaneColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFDCEBF5).toArgb()))
        val purchasePaneColor = ColorScheme(light = ColorInfo.Hex(Color(0xFFF5EFDC).toArgb()))
    }
}
