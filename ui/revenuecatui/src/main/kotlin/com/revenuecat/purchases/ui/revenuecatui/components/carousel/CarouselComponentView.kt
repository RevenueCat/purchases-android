@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
internal fun CarouselComponentView(
    style: CarouselComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val carouselState = rememberUpdatedCarouselComponentState(
        style = style,
        paywallState = state,
    )

    if (!carouselState.visible) {
        return
    }

    Text("TODO", modifier = modifier)
}

@Preview
@Composable
private fun CarouselComponentView_Preview() {
    CarouselComponentView(
        style = previewCarouselComponentStyle(),
        state = previewEmptyState(),
    )
}

@Suppress("LongParameterList")
private fun previewCarouselComponentStyle(
    slides: List<StackComponentStyle> = previewSlides(),
    initialSlideIndex: Int = 0,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    size: Size = Size(width = SizeConstraint.Fit, height = SizeConstraint.Fit),
    sidePagePeek: Dp = 8.dp,
    spacing: Dp = 8.dp,
    backgroundColor: Color = Color.Blue,
    padding: PaddingValues = PaddingValues(0.dp),
    margin: PaddingValues = PaddingValues(0.dp),
    shape: Shape = Shape.Rectangle(),
    borderStyle: BorderStyles = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
    shadowStyle: ShadowStyles = ShadowStyles(
        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
        radius = 10.dp,
        x = 0.dp,
        y = 3.dp,
    ),
    pageControl: CarouselComponentStyle.PageControlStyles? = CarouselComponentStyle.PageControlStyles(
        alignment = Alignment.Bottom,
        active = CarouselComponentStyle.IndicatorStyles(
            size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
            spacing = 4.dp,
            color = ColorStyles(light = ColorStyle.Solid(Color.Blue)),
            margin = PaddingValues(0.dp),
        ),
        default = CarouselComponentStyle.IndicatorStyles(
            size = Size(width = SizeConstraint.Fixed(8u), height = SizeConstraint.Fixed(8u)),
            spacing = 4.dp,
            color = ColorStyles(light = ColorStyle.Solid(Color.Gray)),
            margin = PaddingValues(0.dp),
        ),
    ),
    loop: Boolean? = null,
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
    return emptyList()
}
