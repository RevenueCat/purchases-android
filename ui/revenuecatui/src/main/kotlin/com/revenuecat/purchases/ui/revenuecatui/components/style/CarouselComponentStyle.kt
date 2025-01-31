package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCarouselPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles

@Suppress("LongParameterList")
@Immutable
internal data class CarouselComponentStyle(
    @get:JvmSynthetic
    val slides: List<StackComponentStyle>,
    @get:JvmSynthetic
    val initialSlideIndex: Int,
    @get:JvmSynthetic
    val alignment: Alignment.Vertical,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val sidePagePeek: Dp,
    @get:JvmSynthetic
    val spacing: Dp,
    @get:JvmSynthetic
    val backgroundColor: ColorStyles?,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val shape: Shape,
    @get:JvmSynthetic
    val border: BorderStyles?,
    @get:JvmSynthetic
    val shadow: ShadowStyles?,
    @get:JvmSynthetic
    val pageControl: PageControlStyles?,
    @get:JvmSynthetic
    val loop: Boolean?,
    @get:JvmSynthetic
    val autoAdvance: CarouselComponent.AutoAdvanceSlides?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedCarouselPartial>?,
) : ComponentStyle {
    @Immutable
    data class PageControlStyles(
        @get:JvmSynthetic
        val alignment: Alignment.Vertical,
        @get:JvmSynthetic
        val active: IndicatorStyles,
        @get:JvmSynthetic
        val default: IndicatorStyles,
    )

    @Immutable
    data class IndicatorStyles(
        @get:JvmSynthetic
        val size: Size,
        @get:JvmSynthetic
        val spacing: Dp,
        @get:JvmSynthetic
        val color: ColorStyles,
        @get:JvmSynthetic
        val margin: PaddingValues,
    )
}
