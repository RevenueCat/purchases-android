@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.offset
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
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.END
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
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
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.SimpleBottomSheetScaffold
import com.revenuecat.purchases.ui.revenuecatui.composables.SimpleSheetState
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallPackageSelectionSheetClose
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallPackageSelectionSheetOpen
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import java.net.URL
import java.util.Date

@Composable
internal fun LoadedPaywallComponents(
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    val configuration = LocalConfiguration.current
    state.update(localeList = configuration.locales)

    val onClick: suspend (PaywallAction) -> Unit = { action ->
        handleClick(action, state, clickHandler, componentInteractionTracker)
    }

    PaywallComponentsScaffold(
        state = state,
        clickHandler = clickHandler,
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
    ) { hasHeaderOverlay ->
        ComponentView(
            style = state.stack,
            state = state,
            onClick = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .conditional(hasHeaderOverlay && !state.mainStackHasHeroImage) {
                    headerTopPadding(state)
                },
        )
    }
}

/**
 * Shared scaffold for all Components-based paywall variants. Handles the background, bottom-sheet,
 * overlay, header, and sticky footer. Callers supply the main scrollable content as [mainContent],
 * which receives [hasHeaderOverlay] so it can apply the correct top-padding offset.
 */
@Composable
internal fun PaywallComponentsScaffold(
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    modifier: Modifier = Modifier,
    mainContent: @Composable (hasHeaderOverlay: Boolean) -> Unit,
) {
    val background = rememberBackgroundStyle(state.background)
    val headerComponentStyle = state.header
    val onClick: suspend (PaywallAction) -> Unit = { action ->
        handleClick(action, state, clickHandler, componentInteractionTracker)
    }

    SimpleBottomSheetScaffold(
        sheetState = state.sheet,
        modifier = modifier.background(background),
    ) {
        WithOptionalBackgroundOverlay(state, background = background) {
            Column {
                HeaderOverlayLayout(
                    state = state,
                    modifier = Modifier.weight(1f),
                ) {
                    // Child 0: caller-supplied main content (scrollable body or slide container).
                    mainContent(hasHeaderOverlay = headerComponentStyle != null)
                    // Child 1 (optional): header overlay — measured first by HeaderOverlayLayout.
                    headerComponentStyle?.let { headerStyle ->
                        ComponentView(
                            style = headerStyle,
                            state = state,
                            onClick = onClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                state.stickyFooter?.let { footerStyle ->
                    ComponentView(
                        style = footerStyle,
                        state = state,
                        onClick = onClick,
                        componentInteractionTracker = componentInteractionTracker,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Custom Layout that measures the header overlay first, stores its pixel height in [state],
 * then measures the main content. This ensures the header height is available during the main
 * content's layout phase without requiring a second composition pass.
 *
 * Children: index 0 = main scrollable content, index 1 (optional) = header overlay.
 */
@Composable
internal fun HeaderOverlayLayout(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        // Measure header first (child 1) to get its height before the main content is measured.
        val headerPlaceable = if (measurables.size > 1) {
            measurables[1].measure(constraints.copy(minHeight = 0))
        } else {
            null
        }

        // Store header height so child Modifier.layout blocks can read it in this same pass.
        // Both hero (ZLayer reads it) and non-hero (headerTopPadding reads it) cases need this.
        state.headerHeightPx = headerPlaceable?.height ?: 0

        // Measure main content. Its inner Modifier.layout blocks can now read state.headerHeightPx.
        val mainPlaceable = measurables[0].measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            mainPlaceable.place(0, 0)
            headerPlaceable?.place(0, 0)
        }
    }
}

/**
 * Adds top padding equal to the header's measured height in pixels. The value is read during the
 * layout phase from [state.headerHeightPx][PaywallState.Loaded.Components.headerHeightPx], which
 * is set by [HeaderOverlayLayout] earlier in the same layout pass.
 */
internal fun Modifier.headerTopPadding(state: PaywallState.Loaded.Components): Modifier =
    this.layout { measurable, constraints ->
        val topPad = state.headerHeightPx
        val placeable = measurable.measure(constraints.offset(vertical = -topPad))
        layout(placeable.width, placeable.height + topPad) {
            placeable.place(0, topPad)
        }
    }

internal suspend fun handleClick(
    action: PaywallAction,
    state: PaywallState.Loaded.Components,
    externalClickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
) {
    when (action) {
        is PaywallAction.External -> externalClickHandler(action)
        is PaywallAction.Internal -> when (action) {
            is PaywallAction.Internal.NavigateTo -> when (action.destination) {
                is PaywallAction.Internal.NavigateTo.Destination.Sheet -> {
                    val sheet = action.destination.sheet
                    componentInteractionTracker.track(
                        paywallPackageSelectionSheetOpen(
                            sheetComponentName = sheet.name,
                            rootSelectedPackage = state.selectedPackageInfo?.rcPackage,
                        ),
                    )
                    state.sheet.show(
                        sheet,
                        state,
                        componentInteractionTracker,
                    ) {
                        handleClick(it, state, externalClickHandler, componentInteractionTracker)
                    }
                }
            }
        }
    }
}

/**
 * Shows the provided [sheet] as this [SimpleSheetState]'s sheet content.
 */
internal fun SimpleSheetState.show(
    sheet: ButtonComponentStyle.Action.NavigateTo.Destination.Sheet,
    state: PaywallState.Loaded.Components,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    onClick: suspend (PaywallAction) -> Unit,
) {
    show(
        backgroundBlur = sheet.backgroundBlur,
        content = {
            ComponentView(
                style = sheet.stack,
                state = state,
                componentInteractionTracker = componentInteractionTracker,
                onClick = { action ->
                    when (action) {
                        is PaywallAction.External.NavigateBack -> hide()
                        else -> onClick(action)
                    }
                },
                modifier = Modifier
                    .applyIfNotNull(sheet.size) { size(it) }
                    .conditional(sheet.size == null) { fillMaxWidth() },
            )
        },
        onDismiss = {
            val sheetSelected = state.selectedPackageInfo
            val resulting = state.peekSelectedPackageInfoAfterSheetDismiss()
            componentInteractionTracker.track(
                paywallPackageSelectionSheetClose(
                    sheetComponentName = sheet.name,
                    sheetSelectedPackage = sheetSelected?.rcPackage,
                    resultingRootPackage = resulting?.rcPackage,
                ),
            )
            state.resetToDefaultPackage()
        },
    )
}

@Suppress("LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun LoadedPaywallComponents_Preview() {
    val state = previewHelloWorldPaywallState()
    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
        modifier = Modifier
            .fillMaxSize(),
    )
}

@Preview
@Composable
private fun LoadedPaywallComponents_BottomSheet_NullSize_Preview() {
    val state = previewHelloWorldPaywallState()

    state.sheet.show(
        sheet = previewBottomSheet(size = null),
        state = state,
        componentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
        onClick = { },
    )

    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
        modifier = Modifier
            .fillMaxSize(),
    )
}

@Preview
@Composable
private fun LoadedPaywallComponents_BottomSheet_FitSize_Preview() {
    val state = previewHelloWorldPaywallState()

    state.sheet.show(
        sheet = previewBottomSheet(size = Size(width = Fit, height = Fit)),
        state = state,
        componentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
        onClick = { },
    )

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
        id = "preview_paywall_id",
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        StackComponent(
                            components = listOf(TestData.Components.monthlyPackageComponent),
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
                                    fontSize = 28,
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

@Suppress("LongMethod")
@Composable
private fun previewHelloWorldPaywallState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        id = "preview_paywall_id",
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
                        TestData.Components.monthlyPackageComponent,
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
                            corners = CornerRadiuses.Dp(
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

private fun previewBottomSheet(
    size: Size? = null,
) =
    ButtonComponentStyle.Action.NavigateTo.Destination.Sheet(
        id = "",
        name = "",
        stack = previewStackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "This is a bottom sheet.",
                ),
                previewTextComponentStyle(
                    text = "This is a bottom sheet.",
                ),
                previewTextComponentStyle(
                    text = "This is a bottom sheet.",
                ),
            ),
            background = BackgroundStyles.Color(
                color = ColorStyles(light = ColorStyle.Solid(Color.White)),
            ),
            border = null,
            shape = Shape.Rectangle(
                corners = CornerRadiuses.Dp(
                    topLeading = 16.0,
                    topTrailing = 16.0,
                    bottomLeading = 0.0,
                    bottomTrailing = 0.0,
                ),
            ),
        ),
        backgroundBlur = true,
        size = size,
    )
