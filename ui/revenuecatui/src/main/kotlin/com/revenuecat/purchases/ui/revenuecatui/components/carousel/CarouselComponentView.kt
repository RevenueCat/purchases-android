@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.CarouselPageChangeInteraction
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallCarouselPageChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.lerp as lerpUnit

// Two is the minimum that keeps the trailing page_peek populated during the wrap. Matches the web SDK.
private const val LOOP_CLONE_PAD = 2

@Suppress("LongMethod")
@JvmSynthetic
@Composable
internal fun CarouselComponentView(
    style: CarouselComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    val carouselState = rememberUpdatedCarouselComponentState(
        style = style,
        paywallState = state,
    )

    if (!carouselState.visible) {
        return
    }

    val backgroundColorStyle = carouselState.background?.let { rememberBackgroundStyle(it) }
    val borderStyle = carouselState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = carouselState.shadow?.let { rememberShadowStyle(shadow = it) }

    val pageCount = style.pages.size

    // The ring must stay finite: every pager index is a distinct LazyLayout key, so an unbounded
    // index space rebuilds a page from scratch on every advance.
    val clonePad = loopClonePad(carouselState.loop, pageCount)
    val ringCount = pageCount + 2 * clonePad
    val initialPage = clonePad + carouselState.initialPageIndex.coerceIn(0, maxOf(pageCount - 1, 0))

    val pagerState = rememberPagerState(initialPage = initialPage) { ringCount }

    val currentLogicalPage by remember(pagerState, clonePad, pageCount) {
        derivedStateOf { carouselLogicalPage(pagerState.currentPage, clonePad, pageCount) }
    }

    val skipProgrammaticPageTracking = remember { ProgrammaticPageTrackingFlag() }

    RecenterLoopClones(pagerState = pagerState, clonePad = clonePad, pageCount = pageCount)

    carouselState.autoAdvance?.let { autoAdvance ->
        EnableAutoAdvance(
            autoAdvance,
            pagerState,
            ringCount,
            skipProgrammaticPageTracking,
        )
    }

    if (pageCount > 0) {
        LaunchedEffect(
            pagerState,
            pageCount,
            clonePad,
            style.componentName,
            style.pageContextNames,
            style.initialPageIndex,
            componentInteractionTracker,
        ) {
            // Logical page, so the invisible clone re-centre does not emit a page change.
            var previousPage = carouselLogicalPage(pagerState.currentPage, clonePad, pageCount)
            snapshotFlow { carouselLogicalPage(pagerState.currentPage, clonePad, pageCount) }.collect { page ->
                if (page != previousPage) {
                    if (skipProgrammaticPageTracking.consumeShouldSkipPageChange()) {
                        // Auto-advance scroll; do not emit component interaction.
                    } else {
                        fun pageName(logical: Int): String? =
                            style.pageContextNames.getOrNull(logical)?.takeUnless { it.isBlank() }
                        componentInteractionTracker.track(
                            paywallCarouselPageChange(
                                CarouselPageChangeInteraction(
                                    componentName = style.componentName,
                                    destinationPageIndex = page,
                                    originPageIndex = previousPage,
                                    defaultPageIndex = style.initialPageIndex,
                                    originContextName = pageName(previousPage),
                                    destinationContextName = pageName(page),
                                ),
                            ),
                        )
                    }
                    previousPage = page
                }
            }
        }
    }

    // A Fit carousel sizes to its tallest page, but each page is measured before that height is
    // known, so a Fill page wraps its own (smaller) content instead of matching. Pin the Fill pages
    // to the measured height; leave the Pager unpinned so it keeps tracking the tallest page as
    // content grows (async WebView), which avoids latching to the first frame.
    // A SubcomposeLayout single pass would avoid this measure -> recompose reflow, but it would
    // compose every page (WebViews included) twice, so it isn't worth it for one settle frame.
    val density = LocalDensity.current
    var pagerHeightPx by remember(carouselState.pages) { mutableIntStateOf(0) }
    val fillPageModifier = fillPageModifierOrEmpty(carouselState.size.height, pagerHeightPx, density)

    Column(
        modifier = modifier
            .size(carouselState.size)
            .padding(carouselState.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, carouselState.shape) }
            .applyIfNotNull(backgroundColorStyle) { background(it, carouselState.shape) }
            .clip(carouselState.shape)
            .applyIfNotNull(borderStyle) {
                border(it, carouselState.shape)
                    .padding(it.width)
            }
            .padding(carouselState.padding),
    ) {
        val pageControl = @Composable {
            carouselState.pageControl?.let {
                PagerIndicator(
                    pageControl = it,
                    pageCount = pageCount,
                    currentPage = currentLogicalPage,
                    pagerState = pagerState,
                )
            }
        }

        if (carouselState.pageControl?.position == CarouselComponent.PageControl.Position.TOP) {
            pageControl()
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = carouselState.pagePeek + carouselState.pageSpacing),
            // Compose the whole ring: keeps the pager correctly sized and stops pages being torn
            // down. Ceiling: a looping carousel holds pageCount + 4 live pages, WebViews included.
            beyondViewportPageCount = ringCount,
            pageSpacing = carouselState.pageSpacing,
            verticalAlignment = carouselState.pageAlignment,
            modifier = Modifier.onSizeChanged { pagerHeightPx = it.height },
        ) { page ->
            val pageStyle = carouselState.pages[carouselLogicalPage(page, clonePad, pageCount)]
            StackComponentView(
                style = pageStyle,
                state = state,
                clickHandler = clickHandler,
                componentInteractionTracker = componentInteractionTracker,
                modifier = pageHeightModifier(pageStyle.size.height, fillPageModifier),
            )
        }

        if (carouselState.pageControl?.position == CarouselComponent.PageControl.Position.BOTTOM) {
            pageControl()
        }
    }
}

@Composable
private fun ColumnScope.PagerIndicator(
    pageControl: CarouselComponentStyle.PageControlStyles,
    pageCount: Int,
    currentPage: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val backgroundColorStyle = pageControl.backgroundColor?.forCurrentTheme
    val borderStyle = pageControl.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = pageControl.shadow?.let { rememberShadowStyle(shadow = it) }
    val composeShape by remember(pageControl.shape) { derivedStateOf { pageControl.shape.toShape() } }

    Row(
        modifier = modifier
            .align(Alignment.CenterHorizontally)
            .padding(pageControl.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
            .applyIfNotNull(backgroundColorStyle) { background(it, composeShape) }
            .applyIfNotNull(borderStyle) { border(it, composeShape) }
            .padding(pageControl.padding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { iteration ->
            Indicator(
                pagerState = pagerState,
                pageIndex = iteration,
                currentPage = currentPage,
                pageControl = pageControl,
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun Indicator(
    pagerState: PagerState,
    pageIndex: Int,
    currentPage: Int,
    pageControl: CarouselComponentStyle.PageControlStyles,
) {
    val progress by remember(pageIndex, currentPage) {
        derivedStateOf {
            when {
                pageIndex == currentPage -> {
                    if (pagerState.currentPageOffsetFraction >= 0f) {
                        1f - pagerState.currentPageOffsetFraction
                    } else {
                        1f + pagerState.currentPageOffsetFraction
                    }
                }
                pageIndex == currentPage + 1 && pagerState.currentPageOffsetFraction >= 0f -> {
                    pagerState.currentPageOffsetFraction
                }
                pageIndex == currentPage - 1 && pagerState.currentPageOffsetFraction < 0f -> {
                    -pagerState.currentPageOffsetFraction
                }
                else -> 0f
            }
        }
    }

    // Plain vals, not remembered: the body already reads `progress` on every recomposition for
    // `color`, and a keyless remember here would pin the `progress` state object from the first
    // composition, freezing the dot sizes on the page the carousel happened to start on.
    val targetWidth = lerpUnit(pageControl.default.width, pageControl.active.width, progress)
    val targetHeight = lerpUnit(pageControl.default.height, pageControl.active.height, progress)
    val targetStrokeWidth = lerpUnit(
        pageControl.default.strokeWidth ?: 0.dp,
        pageControl.active.strokeWidth ?: 0.dp,
        progress,
    )

    val width by animateDpAsState(
        targetValue = targetWidth,
    )
    val height by animateDpAsState(
        targetValue = targetHeight,
    )

    val color = lerp(
        (pageControl.default.color.forCurrentTheme as? ColorStyle.Solid)?.color ?: Color.Transparent,
        (pageControl.active.color.forCurrentTheme as? ColorStyle.Solid)?.color ?: Color.Transparent,
        progress,
    )

    val shouldApplyStroke = (pageControl.default.strokeColor != null || pageControl.active.strokeColor != null) &&
        (pageControl.default.strokeWidth != null || pageControl.active.strokeWidth != null)

    val strokeColor = lerp(
        (pageControl.default.strokeColor?.forCurrentTheme as? ColorStyle.Solid)?.color ?: Color.Transparent,
        (pageControl.active.strokeColor?.forCurrentTheme as? ColorStyle.Solid)?.color ?: Color.Transparent,
        progress,
    )

    val strokeWidth by animateDpAsState(
        targetValue = targetStrokeWidth,
    )

    Box(
        modifier = Modifier
            .padding(horizontal = pageControl.spacing / 2)
            .clip(Shape.Pill.toShape())
            .background(color)
            .size(width = width, height = height)
            .conditional(shouldApplyStroke) {
                border(width = strokeWidth, color = strokeColor, shape = Shape.Pill.toShape())
            },
    )
}

/**
 * Snaps back into the real zone when the pager settles on a clone. The clone holds identical
 * content, so the jump is invisible.
 */
@Composable
private fun RecenterLoopClones(pagerState: PagerState, clonePad: Int, pageCount: Int) {
    if (clonePad == 0) return
    LaunchedEffect(pagerState, clonePad, pageCount) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            carouselRecenterTarget(settledPage, clonePad, pageCount)?.let { pagerState.scrollToPage(it) }
        }
    }
}

@Composable
private fun EnableAutoAdvance(
    autoAdvance: CarouselComponent.AutoAdvancePages,
    pagerState: PagerState,
    ringCount: Int,
    skipProgrammaticPageTracking: ProgrammaticPageTrackingFlag,
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(autoAdvance.msTimePerPage.toLong())
            if (!pagerState.isScrollInProgress) {
                val nextPage = nextAutoAdvanceTargetPage(
                    ringCount = ringCount,
                    currentPage = pagerState.currentPage,
                )
                if (nextPage != null) {
                    skipProgrammaticPageTracking.markProgrammaticScrollStarted()
                    try {
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(
                                autoAdvance.msTransitionTime,
                            ),
                        )
                    } catch (_: CancellationException) {
                        skipProgrammaticPageTracking.clear()
                        // Do nothing, so we continue scrolling on the next loop
                    }
                }
            }
        }
    }
}

/**
 * One-shot flag used to suppress tracking for the next programmatic carousel page change.
 */
private class ProgrammaticPageTrackingFlag {
    private var shouldSkipNextPageChange = false

    fun markProgrammaticScrollStarted() {
        shouldSkipNextPageChange = true
    }

    fun consumeShouldSkipPageChange(): Boolean {
        val shouldSkip = shouldSkipNextPageChange
        shouldSkipNextPageChange = false
        return shouldSkip
    }

    fun clear() {
        shouldSkipNextPageChange = false
    }
}

internal fun loopClonePad(loop: Boolean, pageCount: Int): Int =
    if (loop && pageCount > 1) LOOP_CLONE_PAD else 0

/** Next pager index for auto-advance, or `null` at the end of the index space. */
internal fun nextAutoAdvanceTargetPage(ringCount: Int, currentPage: Int): Int? =
    (currentPage + 1).takeIf { it < ringCount }

/**
 * Logical page shown at [pagerIndex], mapping the clone-padded ring back onto `0..pageCount-1`.
 * A server-sent carousel can have no pages at all, and `mod(0)` throws.
 */
internal fun carouselLogicalPage(pagerIndex: Int, clonePad: Int, pageCount: Int): Int =
    if (pageCount <= 0) 0 else (pagerIndex - clonePad).mod(pageCount)

/**
 * Pager index holding the same content as [settledPage] but inside the ring's real zone, or `null`
 * when it already is.
 */
internal fun carouselRecenterTarget(settledPage: Int, clonePad: Int, pageCount: Int): Int? = when {
    settledPage < clonePad -> settledPage + pageCount
    settledPage >= clonePad + pageCount -> settledPage - pageCount
    else -> null
}

// Only a Fit carousel leaves pages unbounded at measure time; Fixed/Fill already bound them.
private fun fillPageModifierOrEmpty(carouselHeight: SizeConstraint, measuredHeightPx: Int, density: Density): Modifier =
    if (carouselHeight is SizeConstraint.Fit && measuredHeightPx > 0) {
        Modifier.height(with(density) { measuredHeightPx.toDp() })
    } else {
        Modifier
    }

// Pin only Fill pages: a Fit/Fixed page resolves its own height, and pinning it would feed a
// sibling's height back into itself.
private fun pageHeightModifier(pageHeight: SizeConstraint, fillPageModifier: Modifier): Modifier =
    if (pageHeight is SizeConstraint.Fill) fillPageModifier else Modifier

@Preview
@Composable
private fun CarouselComponentView_Preview() {
    Box(modifier = Modifier.background(Color.White)) {
        CarouselComponentView(
            style = previewCarouselComponentStyle(),
            state = previewEmptyState(),
            clickHandler = {},
        )
    }
}

@Preview
@Composable
private fun CarouselComponentView_Top_Preview() {
    Box(modifier = Modifier.background(Color.White)) {
        CarouselComponentView(
            style = previewCarouselComponentStyle(
                pageControl = previewPageControl(CarouselComponent.PageControl.Position.TOP),
            ),
            state = previewEmptyState(),
            clickHandler = {},
        )
    }
}

@Preview
@Composable
private fun CarouselComponentView_Loop_Preview() {
    Box(modifier = Modifier.background(Color.White)) {
        CarouselComponentView(
            style = previewCarouselComponentStyle(
                loop = true,
                autoAdvance = CarouselComponent.AutoAdvancePages(
                    msTimePerPage = 1000,
                    msTransitionTime = 500,
                    transitionType = CarouselComponent.AutoAdvancePages.TransitionType.FADE,
                ),
            ),
            state = previewEmptyState(),
            clickHandler = {},
        )
    }
}

@Suppress("LongParameterList")
private fun previewCarouselComponentStyle(
    pages: List<StackComponentStyle> = previewPages(),
    initialPageIndex: Int = 0,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    visible: Boolean = true,
    size: Size = Size(width = SizeConstraint.Fit(), height = SizeConstraint.Fit()),
    sidePagePeek: Dp = 20.dp,
    spacing: Dp = 8.dp,
    backgroundColor: Color = Color.LightGray,
    padding: PaddingValues = PaddingValues(0.dp),
    margin: PaddingValues = PaddingValues(vertical = 16.dp),
    shape: Shape = Shape.Rectangle(),
    borderStyle: BorderStyles? = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
    shadowStyle: ShadowStyles? = ShadowStyles(
        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
        radius = 5.dp,
        x = 0.dp,
        y = 3.dp,
    ),
    pageControl: CarouselComponentStyle.PageControlStyles? = previewPageControl(),
    loop: Boolean = false,
    autoAdvance: CarouselComponent.AutoAdvancePages? = null,
): CarouselComponentStyle {
    return CarouselComponentStyle(
        pages = pages,
        pageContextNames = List(pages.size) { null },
        initialPageIndex = initialPageIndex,
        pageAlignment = alignment,
        visible = visible,
        size = size,
        pagePeek = sidePagePeek,
        pageSpacing = spacing,
        background = BackgroundStyles.Color(
            ColorStyles(
                light = ColorStyle.Solid(backgroundColor),
            ),
        ),
        padding = padding,
        margin = margin,
        shape = shape,
        border = borderStyle,
        shadow = shadowStyle,
        pageControl = pageControl,
        loop = loop,
        autoAdvance = autoAdvance,
        rcPackage = null,
        tabIndex = null,
        overrides = emptyList(),
    )
}

private fun previewPageControl(
    position: CarouselComponent.PageControl.Position = CarouselComponent.PageControl.Position.BOTTOM,
): CarouselComponentStyle.PageControlStyles {
    return CarouselComponentStyle.PageControlStyles(
        position = position,
        spacing = 4.dp,
        padding = PaddingValues(all = 8.dp),
        margin = PaddingValues(all = 8.dp),
        backgroundColor = ColorStyles(
            light = ColorStyle.Solid(Color.Green),
        ),
        shape = Shape.Pill,
        border = BorderStyles(
            width = 4.dp,
            colors = ColorStyles(light = ColorStyle.Solid(Color.Blue)),
        ),
        shadow = ShadowStyles(
            colors = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            radius = 20.dp,
            x = 8.dp,
            y = 8.dp,
        ),
        active = CarouselComponentStyle.IndicatorStyles(
            width = 14.dp,
            height = 10.dp,
            color = ColorStyles(light = ColorStyle.Solid(Color.Blue)),
            strokeColor = ColorStyles(light = ColorStyle.Solid(Color.Red)),
            strokeWidth = 2.dp,
        ),
        default = CarouselComponentStyle.IndicatorStyles(
            width = 8.dp,
            height = 8.dp,
            color = ColorStyles(light = ColorStyle.Solid(Color.Gray)),
            strokeColor = null,
            strokeWidth = null,
        ),
    )
}

private fun previewPages(): List<StackComponentStyle> {
    return listOf(
        previewPage("Page 1", Color.Red, height = 200u),
        previewPage("Page 2", Color.Green, height = 100u),
        previewPage("Page 3", Color.Blue, height = 300u),
        previewPage("Page 4", Color.Yellow, height = 200u),
    )
}

private fun previewPage(
    pageText: String,
    backgroundColor: Color,
    height: UInt,
): StackComponentStyle {
    return StackComponentStyle(
        children = listOf(
            previewTextComponentStyle(
                text = pageText,
            ),
        ),
        dimension = Dimension.Vertical(
            alignment = HorizontalAlignment.CENTER,
            distribution = FlexDistribution.CENTER,
        ),
        visible = true,
        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(height)),
        spacing = 8.dp,
        background = BackgroundStyles.Color(
            color = ColorStyles(light = ColorStyle.Solid(backgroundColor)),
        ),
        padding = PaddingValues(vertical = 16.dp),
        margin = PaddingValues(0.dp),
        shape = Shape.Rectangle(),
        border = null,
        shadow = null,
        badge = null,
        scrollOrientation = null,
        rcPackage = null,
        tabIndex = null,
        countdownDate = null,
        countFrom = CountdownComponent.CountFrom.DAYS,
        overrides = emptyList(),
    )
}
