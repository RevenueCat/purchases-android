package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.InternalRevenueCatAPI
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
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.MILLIS_2025_01_25
import com.revenuecat.purchases.ui.revenuecatui.components.previewUiConfig
import com.revenuecat.purchases.ui.revenuecatui.components.validatePaywallComponentsDataOrNullForPreviews
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL
import java.util.Date

/**
 * Verifies that a semi-transparent sticky footer overlaps the scrollable main content: the footer is
 * pinned to the bottom and drawn on top, the body shows through it, and the content reserves bottom
 * clearance equal to the footer height.
 */
@OptIn(InternalRevenueCatAPI::class)
@RunWith(Parameterized::class)
internal class OverlappingFooterSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {

    @Test
    fun transparentFooterOverlapsScrollableContent() {
        screenshotTest {
            OverlappingFooterPaywall()
        }
    }

    @Suppress("LongMethod")
    @Composable
    private fun OverlappingFooterPaywall() {
        val textColor = ColorScheme(
            light = ColorInfo.Hex(Color(0xFF272727).toArgb()),
            dark = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()),
        )
        val backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()),
            dark = ColorInfo.Hex(Color(0xFF121212).toArgb()),
        )
        // Semi-transparent, tinted footer background so the scrolled content is clearly visible THROUGH
        // the footer (a distinct tint over the content behind it), demonstrating the overlap.
        val translucentFooterColor = ColorScheme(
            light = ColorInfo.Hex(Color(0x99057C5B).toArgb()),
            dark = ColorInfo.Hex(Color(0x99057C5B).toArgb()),
        )
        val ctaTextColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))
        val ctaBackgroundColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF057C5B).toArgb()))

        val featureRows = (1..25).map { index ->
            TextComponent(
                text = LocalizationKey("feature-$index"),
                color = textColor,
                horizontalAlignment = LEADING,
                size = Size(width = Fill, height = Fit),
                margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
            )
        }

        val data = PaywallComponentsData(
            id = "snapshot_transparent_footer",
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
                                fontSize = 28,
                                horizontalAlignment = LEADING,
                                size = Size(width = Fill, height = Fit),
                                margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 0.0),
                            ),
                        ) + featureRows,
                        dimension = Vertical(alignment = LEADING, distribution = START),
                        size = Size(width = Fill, height = Fill),
                        padding = Padding(top = 32.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
                    ),
                    background = Background.Color(backgroundColor),
                    stickyFooter = StickyFooterComponent(
                        stack = StackComponent(
                            components = listOf(
                                StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("cta"),
                                            color = ctaTextColor,
                                            fontWeight = FontWeight.BOLD,
                                        ),
                                    ),
                                    dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                                    size = Size(width = Fill, height = Fit),
                                    backgroundColor = ctaBackgroundColor,
                                    padding = Padding(top = 16.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
                                    shape = Shape.Pill,
                                ),
                            ),
                            dimension = Vertical(alignment = CENTER, distribution = START),
                            size = Size(width = Fill, height = Fit),
                            backgroundColor = translucentFooterColor,
                            padding = Padding(top = 16.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
                        ),
                    ),
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to (
                    mapOf(
                        LocalizationKey("title") to LocalizationData.Text("Unlock everything"),
                        LocalizationKey("cta") to LocalizationData.Text("Continue"),
                    ) + (1..25).associate { index ->
                        LocalizationKey("feature-$index") to LocalizationData.Text("✓ Premium feature number $index")
                    }
                    ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "transparent_footer",
            serverDescription = "Transparent overlapping footer",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = "US",
            dateProvider = { Date(MILLIS_2025_01_25) },
            purchases = MockPurchasesType(),
        )

        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
