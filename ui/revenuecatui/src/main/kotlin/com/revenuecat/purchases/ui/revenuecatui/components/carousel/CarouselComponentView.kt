@file:JvmSynthetic
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.CarouselComponent
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.lerp as lerpUnit

@Suppress("LongMethod")
@JvmSynthetic
@Composable
internal fun CarouselComponentView(
    style: CarouselComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
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

    val initialPage = getInitialPage(carouselState)

    val pagerState = rememberPagerState(initialPage = initialPage) {
        if (carouselState.loop) {
            Int.MAX_VALUE
        } else {
            pageCount
        }
    }

    carouselState.autoAdvance?.let { autoAdvance ->
        EnableAutoAdvance(autoAdvance, pagerState, carouselState.loop, pageCount)
    }

    Column(
        modifier = modifier
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
            // This will load all the pages at once, which allows the pager to always have the correct size
            beyondViewportPageCount = pageCount,
            pageSpacing = carouselState.pageSpacing,
            verticalAlignment = carouselState.pageAlignment,
        ) { page ->
            StackComponentView(
                style = carouselState.pages[page % pageCount],
                state = state,
                clickHandler = clickHandler,
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
                pageCount = pageCount,
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
    pageCount: Int,
    pageControl: CarouselComponentStyle.PageControlStyles,
) {
    val progress by remember {
        derivedStateOf {
            val processedCurrentPage = pagerState.currentPage % pageCount
            when {
                pageIndex == processedCurrentPage -> {
                    if (pagerState.currentPageOffsetFraction >= 0f) {
                        1f - pagerState.currentPageOffsetFraction
                    } else {
                        1f + pagerState.currentPageOffsetFraction
                    }
                }
                pageIndex == processedCurrentPage + 1 && pagerState.currentPageOffsetFraction >= 0f -> {
                    pagerState.currentPageOffsetFraction
                }
                pageIndex == processedCurrentPage - 1 && pagerState.currentPageOffsetFraction < 0f -> {
                    -pagerState.currentPageOffsetFraction
                }
                else -> 0f
            }
        }
    }

    val targetWidth by remember {
        derivedStateOf {
            lerpUnit(
                pageControl.default.width,
                pageControl.active.width,
                progress,
            )
        }
    }
    val targetHeight by remember {
        derivedStateOf {
            lerpUnit(
                pageControl.default.height,
                pageControl.active.height,
                progress,
            )
        }
    }

    val targetStrokeWidth by remember {
        derivedStateOf {
            lerpUnit(
                pageControl.default.strokeWidth ?: 0.dp,
                pageControl.active.strokeWidth ?: 0.dp,
                progress,
            )
        }
    }

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

@Composable
private fun EnableAutoAdvance(
    autoAdvance: CarouselComponent.AutoAdvancePages,
    pagerState: PagerState,
    shouldLoop: Boolean,
    pageCount: Int,
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(autoAdvance.msTimePerPage.toLong())
            if (pagerState.isScrollInProgress) continue
            val nextPage = if (shouldLoop) {
                pagerState.currentPage + 1
            } else {
                (pagerState.currentPage + 1) % pageCount
            }
            try {
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        autoAdvance.msTransitionTime,
                    ),
                )
            } catch (_: CancellationException) {
                // Do nothing, so we continue scrolling on the next loop
            }
        }
    }
}

private fun getInitialPage(carouselState: CarouselComponentState) = if (carouselState.loop) {
    // When looping, we use a very large number of pages to allow for "infinite" scrolling
    // We need to calculate the initial page index in the middle of that large number of pages to make the carousel
    // start at the correct page
    var currentPage = Int.MAX_VALUE / 2
    while ((currentPage % carouselState.pages.size) != carouselState.initialPageIndex) {
        currentPage++
    }
    currentPage
} else {
    carouselState.initialPageIndex
}

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
    size: Size = Size(width = SizeConstraint.Fit, height = SizeConstraint.Fit),
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
        overrides = emptyList(),
    )
}
