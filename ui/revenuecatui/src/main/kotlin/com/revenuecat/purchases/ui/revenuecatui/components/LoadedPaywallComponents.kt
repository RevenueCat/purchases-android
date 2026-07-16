@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import android.content.res.Configuration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
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
import com.revenuecat.purchases.ui.revenuecatui.helpers.toLayoutDirection
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

    val shouldWrapMainContentInVerticalScroll = shouldWrapMainContentInVerticalScroll(state.stack)
    val mainScrollState = rememberScrollState()
    val layoutDirection = remember(state.locale) {
        state.locale.toJavaLocale().toLayoutDirection()
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        PaywallComponentsScaffold(
            state = state,
            modifier = modifier,
            headerContent = state.header?.let { headerStyle ->
                {
                    ComponentView(
                        style = headerStyle,
                        state = state,
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            footerContent = state.stickyFooter?.let { footerStyle ->
                {
                    ComponentView(
                        style = footerStyle,
                        state = state,
                        onClick = onClick,
                        componentInteractionTracker = componentInteractionTracker,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        ) {
            ComponentView(
                style = state.stack,
                state = state,
                onClick = onClick,
                componentInteractionTracker = componentInteractionTracker,
                modifier = Modifier
                    .fillMaxSize()
                    .conditional(shouldWrapMainContentInVerticalScroll) {
                        verticalScroll(mainScrollState)
                    }
                    .conditional(state.header != null && !state.mainStackHasHeroImage) {
                        headerTopPadding(state)
                    }
                    .conditional(state.stickyFooter != null) {
                        footerBottomPadding(state)
                    },
            )
        }
    }
}

/**
 * Shared scaffold for all Components-based paywall variants. Handles the background, bottom-sheet,
 * and fixed-header overlay layout. Callers supply [mainContent] (the scrollable body) plus optional
 * [headerContent] and [footerContent] composables.
 *
 * Pass [background] = null to skip background painting (e.g. workflow paywalls render per-step
 * backgrounds inside their sliding surfaces).
 */
@Suppress("LongParameterList")
@Composable
internal fun PaywallComponentsScaffold(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    background: BackgroundStyle? = rememberBackgroundStyle(state.background),
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    mainContent: @Composable () -> Unit,
) {
    SimpleBottomSheetScaffold(
        sheetState = state.sheet,
        modifier = background?.let { modifier.background(it) } ?: modifier,
    ) {
        WithOptionalBackgroundOverlay(state, background = background) {
            if (footerContent != null) {
                // Overlapping footer: main content fills the whole area and the footer is pinned to the
                // bottom, drawn on top. Main content reserves bottom clearance equal to the footer height
                // (see footerBottomPadding), so an opaque footer looks identical to the old stacked layout
                // while a transparent footer lets content draw behind it.
                OverlayLayout(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    hasHeader = headerContent != null,
                    hasFooter = true,
                ) {
                    // Child 0: caller-supplied main content.
                    mainContent()
                    // Child 1 (optional): fixed header overlay.
                    headerContent?.invoke()
                    // Child 2: sticky footer overlay, pinned to the bottom.
                    footerContent.invoke()
                }
            } else {
                OverlayLayout(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    hasHeader = headerContent != null,
                ) {
                    // Child 0: caller-supplied main content.
                    mainContent()
                    // Child 1 (optional): fixed header overlay.
                    headerContent?.invoke()
                }
            }
        }
    }
}

/**
 * Custom Layout that measures the fixed header and footer overlays first, stores their pixel heights
 * in [state], then measures the main content so its inner [Modifier.layout] blocks can read those
 * heights in the same pass. The header is placed at the top and the footer pinned to the bottom, both
 * drawn on top of the main content.
 *
 * Only the heights of the overlays this layout owns are written to [state]: [state.headerHeightPx]
 * [PaywallState.Loaded.Components.headerHeightPx] only when [hasHeader], and [state.footerHeightPx]
 * [PaywallState.Loaded.Components.footerHeightPx] only when [hasFooter]. This lets a nested
 * OverlayLayout (workflow steps render one inside the scaffold's, sharing the same state) avoid
 * clobbering a height already set by the outer layout.
 *
 * Children (in emission order): index 0 = main scrollable content, then the optional header overlay
 * (present when [hasHeader]), then the optional footer overlay (present when [hasFooter]).
 */
@Composable
internal fun OverlayLayout(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    hasHeader: Boolean = false,
    hasFooter: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        // Measure the overlays first to get their heights before the main content is measured.
        // Emission order after main content (index 0) is: header (if any), then footer (if any).
        val headerMeasurable = if (hasHeader) measurables[1] else null
        val footerMeasurable = if (hasFooter) measurables[if (hasHeader) 2 else 1] else null

        val headerPlaceable = headerMeasurable?.measure(constraints.copy(minHeight = 0))
        val footerPlaceable = footerMeasurable?.measure(constraints.copy(minHeight = 0))

        // Store overlay heights so child Modifier.layout blocks can read them in this same pass.
        // Header: both hero (ZLayer reads it) and non-hero (headerTopPadding reads it) cases need this.
        // Footer: footerBottomPadding reads it to reserve bottom clearance.
        //
        // Only publish the height of an overlay this layout actually owns. A nested OverlayLayout
        // (workflow steps render one inside the scaffold's, sharing the same state) must not clobber a
        // height set by the outer layout: e.g. an inner layout with hasHeader = false wiping
        // headerHeightPx to 0 would collapse the step's header clearance.
        if (hasHeader) state.headerHeightPx = headerPlaceable?.height ?: 0
        if (hasFooter) state.footerHeightPx = footerPlaceable?.height ?: 0

        // Measure main content. Its inner Modifier.layout blocks can now read the stored heights.
        val mainPlaceable = measurables[0].measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            mainPlaceable.placeRelative(0, 0)
            headerPlaceable?.placeRelative(0, 0)
            footerPlaceable?.placeRelative(0, constraints.maxHeight - footerPlaceable.height)
        }
    }
}

/**
 * Adds top padding equal to the header's measured height in pixels. The value is read during the
 * layout phase from [state.headerHeightPx][PaywallState.Loaded.Components.headerHeightPx], which
 * is set by [OverlayLayout] earlier in the same layout pass.
 */
internal fun Modifier.headerTopPadding(state: PaywallState.Loaded.Components): Modifier =
    this.layout { measurable, constraints ->
        val topPad = state.headerHeightPx
        val placeable = measurable.measure(constraints.offset(vertical = -topPad))
        layout(placeable.width, placeable.height + topPad) {
            placeable.placeRelative(0, topPad)
        }
    }

/**
 * Reserves bottom clearance equal to the sticky footer's measured height in pixels, so the last piece
 * of main content can scroll clear of a footer that content otherwise draws behind. The value is read
 * during the layout phase from [state.footerHeightPx][PaywallState.Loaded.Components.footerHeightPx],
 * which is set by [OverlayLayout] earlier in the same layout pass.
 */
internal fun Modifier.footerBottomPadding(state: PaywallState.Loaded.Components): Modifier =
    this.layout { measurable, constraints ->
        val bottomPad = state.footerHeightPx
        val placeable = measurable.measure(constraints.offset(vertical = -bottomPad))
        layout(placeable.width, placeable.height + bottomPad) {
            // Content placed at the top; the extra space is reserved at the bottom, behind the footer.
            placeable.placeRelative(0, 0)
        }
    }

/**
 * Returns whether the caller should wrap [rootStack] in an outer `verticalScroll` modifier.
 * Returns `false` when the root stack already scrolls vertically (overflow = SCROLL on a vertical
 * dimension), because two vertical scroll modifiers on the same axis crash at runtime.
 */
internal fun shouldWrapMainContentInVerticalScroll(rootStack: ComponentStyle): Boolean =
    (rootStack as? StackComponentStyle)?.scrollOrientation != Orientation.Vertical

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
        contentKey = sheet.id,
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
