package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
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
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import java.net.URL
import java.util.Date

private const val SPLIT_MIN_WIDTH_DP = 700.0
private const val SPLIT_MIN_HEIGHT_DP = 480.0
private const val CONTENT_PANE_COLOR = 0xFFDCEBF5
private const val PURCHASE_PANE_COLOR = 0xFFF5EFDC

@Preview(name = "phone - stacked", device = "spec:width=411dp,height=891dp")
@Preview(name = "phone landscape - stacked (height floor)", device = "spec:width=891dp,height=411dp")
@Preview(name = "tablet - side by side", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun LoadedPaywallComponents_WindowSizeConditions_Preview() {
    LoadedPaywallComponents(
        state = previewWindowSizeConditionsState(),
        clickHandler = { },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Two panes stacked vertically by default, side by side when the window is at least
 * 700x480dp (so landscape phones stay stacked).
 */
@Suppress("LongMethod")
@Composable
private fun previewWindowSizeConditionsState(): PaywallState.Loaded.Components {
    val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
    val contentPane = StackComponent(
        components = listOf(
            TextComponent(
                text = LocalizationKey("split-title"),
                color = textColor,
            ),
        ),
        dimension = Vertical(alignment = CENTER, distribution = START),
        size = Size(width = Fill(), height = Fill()),
        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color(CONTENT_PANE_COLOR).toArgb())),
    )
    val purchasePane = StackComponent(
        components = listOf(
            TextComponent(
                text = LocalizationKey("split-package"),
                color = textColor,
            ),
            TestData.Components.monthlyPackageComponent,
        ),
        dimension = Vertical(alignment = CENTER, distribution = START),
        size = Size(width = Fill(), height = Fill()),
        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color(PURCHASE_PANE_COLOR).toArgb())),
    )
    val data = PaywallComponentsData(
        id = "preview_window_size_conditions",
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(contentPane, purchasePane),
                    dimension = Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill(), height = Fill()),
                    overrides = listOf(
                        ComponentOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.WindowWidthRule(
                                    operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                                    value = SPLIT_MIN_WIDTH_DP,
                                ),
                                ComponentOverride.Condition.WindowHeightRule(
                                    operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                                    value = SPLIT_MIN_HEIGHT_DP,
                                ),
                            ),
                            properties = PartialStackComponent(
                                dimension = Horizontal(alignment = VerticalAlignment.TOP, distribution = START),
                            ),
                        ),
                    ),
                ),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("split-title") to LocalizationData.Text("Experience Pro today!"),
                LocalizationKey("split-package") to LocalizationData.Text("Monthly — $9.99/mo"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "window_size_conditions",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(previewUiConfig(), data),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    return offering.toComponentsPaywallState(
        validationResult = validated,
        storefrontCountryCode = null,
        dateProvider = { Date(MILLIS_2025_01_25) },
        purchases = MockPurchasesType(),
    )
}
