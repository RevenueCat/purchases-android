package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.HeaderComponent
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
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedWorkflowPaywall
import com.revenuecat.purchases.ui.revenuecatui.components.MILLIS_2025_01_25
import com.revenuecat.purchases.ui.revenuecatui.components.previewUiConfig
import com.revenuecat.purchases.ui.revenuecatui.components.validatePaywallComponentsDataOrNullForPreviews
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL
import java.util.Date

/**
 * Snapshots covering the overlapping sticky footer layout:
 * - a transparent footer over long scrollable content (content shows through / scrolls behind it),
 * - a small body that must center in the space *above* the footer (not the whole screen),
 * - an oversized footer taller than half the screen.
 */
@OptIn(InternalRevenueCatAPI::class)
@RunWith(Parameterized::class)
internal class OverlappingFooterSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {

    @Test
    fun transparentFooterOverlapsScrollableContent() {
        screenshotTest { TransparentFooterPaywall() }
    }

    @Test
    fun smallBodyCentersAboveFooter() {
        screenshotTest { SmallCenteredBodyPaywall() }
    }

    @Test
    fun largeFooterTallerThanHalfScreen() {
        screenshotTest { LargeFooterPaywall() }
    }

    @Test
    fun workflowStepWithHeaderAndFooterReservesHeaderClearance() {
        screenshotTest { WorkflowHeaderFooterPaywall() }
    }

    /**
     * Long scrollable body with a semi-transparent, tinted footer. The content is visible THROUGH the
     * footer and scrolls behind it, while reserving footer-height bottom clearance.
     */
    @Composable
    private fun TransparentFooterPaywall() {
        val featureRows = (1..25).map { index ->
            TextComponent(
                text = LocalizationKey("feature-$index"),
                color = textColor,
                horizontalAlignment = LEADING,
                size = Size(width = Fill, height = Fit),
                margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
            )
        }

        ComponentPaywall(
            rootStack = StackComponent(
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
            footerStack = ctaFooterStack(backgroundColor = translucentFooterColor),
            localizations = mapOf(
                LocalizationKey("title") to LocalizationData.Text("Unlock everything"),
                LocalizationKey("cta") to LocalizationData.Text("Continue"),
            ) + (1..25).associate { index ->
                LocalizationKey("feature-$index") to LocalizationData.Text("✓ Premium feature number $index")
            },
        )
    }

    /**
     * A small body vertically centered. It must center within the space above the footer, matching the
     * pre-overlap behavior (footer-height clearance is reserved at the bottom).
     */
    @Composable
    private fun SmallCenteredBodyPaywall() {
        ComponentPaywall(
            rootStack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = LocalizationKey("title"),
                        color = textColor,
                        fontWeight = FontWeight.BOLD,
                        fontSize = 28,
                        horizontalAlignment = CENTER,
                        size = Size(width = Fill, height = Fit),
                        margin = Padding(top = 0.0, bottom = 12.0, leading = 32.0, trailing = 32.0),
                    ),
                    TextComponent(
                        text = LocalizationKey("subtitle"),
                        color = textColor,
                        horizontalAlignment = CENTER,
                        size = Size(width = Fill, height = Fit),
                        margin = Padding(top = 0.0, bottom = 0.0, leading = 32.0, trailing = 32.0),
                    ),
                ),
                dimension = Vertical(alignment = CENTER, distribution = FlexDistribution.CENTER),
                size = Size(width = Fill, height = Fill),
            ),
            footerStack = ctaFooterStack(backgroundColor = translucentFooterColor),
            localizations = mapOf(
                LocalizationKey("title") to LocalizationData.Text("Centered body"),
                LocalizationKey("subtitle") to LocalizationData.Text("This should be centered above the footer"),
                LocalizationKey("cta") to LocalizationData.Text("Continue"),
            ),
        )
    }

    /**
     * A footer taller than half the screen. Content occupies the reduced region above it; the footer
     * fills the rest without clipping the layout.
     */
    @Composable
    private fun LargeFooterPaywall() {
        val footerRows = (1..8).map { index ->
            TextComponent(
                text = LocalizationKey("footer-line-$index"),
                color = ctaTextColor,
                horizontalAlignment = CENTER,
                size = Size(width = Fill, height = Fit),
                margin = Padding(top = 6.0, bottom = 6.0, leading = 0.0, trailing = 0.0),
            )
        }

        ComponentPaywall(
            rootStack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = LocalizationKey("title"),
                        color = textColor,
                        fontWeight = FontWeight.BOLD,
                        fontSize = 28,
                        horizontalAlignment = LEADING,
                        size = Size(width = Fill, height = Fit),
                        margin = Padding(top = 0.0, bottom = 12.0, leading = 0.0, trailing = 0.0),
                    ),
                    TextComponent(
                        text = LocalizationKey("subtitle"),
                        color = textColor,
                        horizontalAlignment = LEADING,
                        size = Size(width = Fill, height = Fit),
                    ),
                ),
                dimension = Vertical(alignment = LEADING, distribution = START),
                size = Size(width = Fill, height = Fill),
                padding = Padding(top = 32.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
            ),
            footerStack = StackComponent(
                components = footerRows + StackComponent(
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
                    margin = Padding(top = 16.0, bottom = 0.0, leading = 0.0, trailing = 0.0),
                    shape = Shape.Pill,
                ),
                dimension = Vertical(alignment = CENTER, distribution = FlexDistribution.CENTER),
                // Fixed height guarantees the footer is taller than half of a phone screen.
                size = Size(width = Fill, height = Fixed(520u)),
                backgroundColor = solidFooterColor,
                padding = Padding(top = 24.0, bottom = 24.0, leading = 32.0, trailing = 32.0),
            ),
            localizations = mapOf(
                LocalizationKey("title") to LocalizationData.Text("Big footer"),
                LocalizationKey("subtitle") to LocalizationData.Text("The footer below is taller than half the screen."),
                LocalizationKey("cta") to LocalizationData.Text("Continue"),
            ) + (1..8).associate { index ->
                LocalizationKey("footer-line-$index") to LocalizationData.Text("Footer line $index")
            },
        )
    }

    /**
     * A workflow step that has BOTH a (non-hero) header and a sticky footer. The step renders its own
     * OverlayLayout nested inside the scaffold's, sharing the same state. Regression guard: the inner
     * layout must not wipe the header height, so the body keeps its header clearance (title sits below
     * the header bar, not under it) while still reserving footer clearance at the bottom.
     */
    @Composable
    private fun WorkflowHeaderFooterPaywall() {
        val featureRows = (1..20).map { index ->
            TextComponent(
                text = LocalizationKey("feature-$index"),
                color = textColor,
                horizontalAlignment = LEADING,
                size = Size(width = Fill, height = Fit),
                margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
            )
        }

        val state = buildComponentsState(
            rootStack = StackComponent(
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
            footerStack = ctaFooterStack(backgroundColor = translucentFooterColor),
            header = HeaderComponent(
                stack = StackComponent(
                    components = listOf(
                        TextComponent(
                            text = LocalizationKey("header"),
                            color = ctaTextColor,
                            fontWeight = FontWeight.BOLD,
                            fontSize = 20,
                            horizontalAlignment = CENTER,
                            size = Size(width = Fill, height = Fit),
                        ),
                    ),
                    dimension = Vertical(alignment = CENTER, distribution = FlexDistribution.CENTER),
                    size = Size(width = Fill, height = Fit),
                    backgroundColor = solidFooterColor,
                    padding = Padding(top = 24.0, bottom = 24.0, leading = 16.0, trailing = 16.0),
                ),
            ),
            localizations = mapOf(
                LocalizationKey("header") to LocalizationData.Text("Header"),
                LocalizationKey("title") to LocalizationData.Text("Unlock everything"),
                LocalizationKey("cta") to LocalizationData.Text("Continue"),
            ) + (1..20).associate { index ->
                LocalizationKey("feature-$index") to LocalizationData.Text("✓ Premium feature number $index")
            },
        )

        LoadedWorkflowPaywall(
            workflowState = WorkflowPaywallUiState(
                currentStepId = "step",
                stepStates = mapOf("step" to state),
            ),
            onTransitionComplete = { },
            clickHandler = { },
            componentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun ComponentPaywall(
        rootStack: StackComponent,
        footerStack: StackComponent,
        localizations: Map<LocalizationKey, LocalizationData>,
    ) {
        LoadedPaywallComponents(
            state = buildComponentsState(rootStack = rootStack, footerStack = footerStack, localizations = localizations),
            clickHandler = { },
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun buildComponentsState(
        rootStack: StackComponent,
        footerStack: StackComponent,
        localizations: Map<LocalizationKey, LocalizationData>,
        header: HeaderComponent? = null,
    ): PaywallState.Loaded.Components {
        val data = PaywallComponentsData(
            id = "snapshot_overlapping_footer",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = rootStack,
                    background = Background.Color(backgroundColor),
                    stickyFooter = StickyFooterComponent(stack = footerStack),
                    header = header,
                ),
            ),
            componentsLocalizations = mapOf(LocaleId("en_US") to localizations),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "overlapping_footer",
            serverDescription = "Overlapping footer",
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

    private fun ctaFooterStack(backgroundColor: ColorScheme): StackComponent =
        StackComponent(
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
            backgroundColor = backgroundColor,
            padding = Padding(top = 16.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
        )

    private companion object {
        val textColor = ColorScheme(
            light = ColorInfo.Hex(Color(0xFF272727).toArgb()),
            dark = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()),
        )
        val backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color(0xFFFDFDFD).toArgb()),
            dark = ColorInfo.Hex(Color(0xFF121212).toArgb()),
        )

        // Semi-transparent, tinted footer background so the content behind it is clearly visible.
        val translucentFooterColor = ColorScheme(
            light = ColorInfo.Hex(Color(0x99057C5B).toArgb()),
            dark = ColorInfo.Hex(Color(0x99057C5B).toArgb()),
        )
        val solidFooterColor = ColorScheme(
            light = ColorInfo.Hex(Color(0xFF057C5B).toArgb()),
            dark = ColorInfo.Hex(Color(0xFF057C5B).toArgb()),
        )
        val ctaTextColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))
        val ctaBackgroundColor = ColorScheme(light = ColorInfo.Hex(Color(0xFF0A3D2E).toArgb()))
    }
}
