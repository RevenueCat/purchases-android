@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.dpOrNull
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

    val backgroundColorStyle = carouselState.backgroundColor?.forCurrentTheme
    val borderStyle = carouselState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = carouselState.shadow?.let { rememberShadowStyle(shadow = it) }

    val pageCount = style.slides.size

    val initialPage = getInitialPage(carouselState)

    val pagerState = rememberPagerState(initialPage = initialPage) {
        if (carouselState.loop) {
            Int.MAX_VALUE
        } else {
            pageCount
        }
    }

    carouselState.autoAdvance?.let { autoAdvance ->
        LaunchedEffect(Unit) {
            while (true) {
                delay(autoAdvance.msTimePerSlide.toLong())
                val nextPage = if (carouselState.loop) {
                    pagerState.currentPage + 1
                } else {
                    (pagerState.currentPage + 1) % pageCount
                }
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        autoAdvance.msTransitionTime,
                    ),
                )
            }
        }
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
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = carouselState.sidePagePeek),
            // This will load all the pages at once, which allows the pager to always have the correct size
            beyondViewportPageCount = pageCount,
            pageSpacing = carouselState.spacing,
            verticalAlignment = carouselState.alignment,
            userScrollEnabled = style.autoAdvance == null,
        ) { page ->
            StackComponentView(
                style = carouselState.slides[page % pageCount],
                state = state,
                clickHandler = clickHandler,
            )
        }

        carouselState.pageControl?.let {
            // The margin between the pager and the indicators is indicated
            // by the margins configured in the indicators themselves
            PagerIndicator(
                pageControl = it,
                pageCount = pageCount,
                pagerState = pagerState,
            )
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
    Row(
        modifier = modifier.align(Alignment.CenterHorizontally),
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
            // We assume the size of the indicator to be Fixed.
            // It won't show otherwise.
            lerpUnit(
                pageControl.default.size.width.dpOrNull() ?: 0.dp,
                pageControl.active.size.width.dpOrNull() ?: 0.dp,
                progress,
            )
        }
    }
    val targetHeight by remember {
        derivedStateOf {
            // We assume the size of the indicator to be Fixed.
            // It won't show otherwise.
            lerpUnit(
                pageControl.default.size.height.dpOrNull() ?: 0.dp,
                pageControl.active.size.height.dpOrNull() ?: 0.dp,
                progress,
            )
        }
    }
    val targetSpacing by remember {
        derivedStateOf {
            lerpUnit(
                pageControl.default.spacing,
                pageControl.active.spacing,
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
    val spacing by animateDpAsState(
        targetValue = targetSpacing,
    )

    val color = lerp(
        (pageControl.default.color.forCurrentTheme as? ColorStyle.Solid)?.color ?: Color.Transparent,
        (pageControl.active.color.forCurrentTheme as? ColorStyle.Solid)?.color ?: Color.Transparent,
        progress,
    )
    val isCurrentPage = pageIndex == (pagerState.currentPage % pageCount)

    Box(
        modifier = Modifier
            // Indicator margin only indicates vertical margins, and we don't want to animate those,
            // or it might jump around during transitions.
            .padding(if (isCurrentPage) pageControl.active.margin else pageControl.default.margin)
            .padding(horizontal = spacing / 2)
            .clip(Shape.Pill.toShape())
            .background(color)
            .size(width = width, height = height),
    )
}

private fun getInitialPage(carouselState: CarouselComponentState) = if (carouselState.loop) {
    // When looping, we use a very large number of pages to allow for "infinite" scrolling
    // We need to calculate the initial page index in the middle of that large number of pages to make the carousel
    // start at the correct slide
    var currentPage = Int.MAX_VALUE / 2
    while ((currentPage % carouselState.slides.size) != carouselState.initialSlideIndex) {
        currentPage++
    }
    currentPage
} else {
    carouselState.initialSlideIndex
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
private fun CarouselComponentView_Loop_Preview() {
    Box(modifier = Modifier.background(Color.White)) {
        CarouselComponentView(
            style = previewCarouselComponentStyle(
                loop = true,
                autoAdvance = CarouselComponent.AutoAdvanceSlides(
                    msTimePerSlide = 1000,
                    msTransitionTime = 500,
                ),
            ),
            state = previewEmptyState(),
            clickHandler = {},
        )
    }
}

@Suppress("LongParameterList")
private fun previewCarouselComponentStyle(
    slides: List<StackComponentStyle> = previewSlides(),
    initialSlideIndex: Int = 0,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
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
    pageControl: CarouselComponentStyle.PageControlStyles? = CarouselComponentStyle.PageControlStyles(
        alignment = Alignment.Bottom,
        active = CarouselComponentStyle.IndicatorStyles(
            size = Size(width = SizeConstraint.Fixed(14u), height = SizeConstraint.Fixed(10u)),
            spacing = 4.dp,
            color = ColorStyles(light = ColorStyle.Solid(Color.Blue)),
            margin = PaddingValues(vertical = 10.dp),
        ),
        default = CarouselComponentStyle.IndicatorStyles(
            size = Size(width = SizeConstraint.Fixed(8u), height = SizeConstraint.Fixed(8u)),
            spacing = 4.dp,
            color = ColorStyles(light = ColorStyle.Solid(Color.Gray)),
            margin = PaddingValues(vertical = 10.dp),
        ),
    ),
    loop: Boolean = false,
    autoAdvance: CarouselComponent.AutoAdvanceSlides? = null,
): CarouselComponentStyle {
    return CarouselComponentStyle(
        slides = slides,
        initialSlideIndex = initialSlideIndex,
        alignment = alignment,
        size = size,
        sidePagePeek = sidePagePeek,
        spacing = spacing,
        backgroundColor = ColorStyles(
            light = ColorStyle.Solid(backgroundColor),
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
        overrides = null,
    )
}

private fun previewSlides(): List<StackComponentStyle> {
    return listOf(
        previewSlide("Slide 1", Color.Red, height = 200u),
        previewSlide("Slide 2", Color.Green, height = 100u),
        previewSlide("Slide 3", Color.Blue, height = 300u),
        previewSlide("Slide 4", Color.Yellow, height = 200u),
    )
}

private fun previewSlide(
    slideText: String,
    backgroundColor: Color,
    height: UInt,
): StackComponentStyle {
    return StackComponentStyle(
        children = listOf(
            previewTextComponentStyle(
                text = slideText,
            ),
        ),
        dimension = Dimension.Vertical(
            alignment = HorizontalAlignment.CENTER,
            distribution = FlexDistribution.CENTER,
        ),
        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(height)),
        spacing = 8.dp,
        backgroundColor = ColorStyles(
            light = ColorStyle.Solid(backgroundColor),
        ),
        padding = PaddingValues(vertical = 16.dp),
        margin = PaddingValues(0.dp),
        shape = Shape.Rectangle(),
        border = null,
        shadow = null,
        badge = null,
        rcPackage = null,
        overrides = null,
    )
}
