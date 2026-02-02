package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCarouselPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles

@Suppress("LongParameterList")
@Immutable
internal data class CarouselComponentStyle(
    @get:JvmSynthetic
    val pages: List<StackComponentStyle>,
    @get:JvmSynthetic
    val initialPageIndex: Int,
    @get:JvmSynthetic
    val pageAlignment: Alignment.Vertical,
    @get:JvmSynthetic
    override val visible: Boolean,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val pagePeek: Dp,
    @get:JvmSynthetic
    val pageSpacing: Dp,
    @get:JvmSynthetic
    val background: BackgroundStyles?,
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
    val loop: Boolean,
    @get:JvmSynthetic
    val autoAdvance: CarouselComponent.AutoAdvancePages?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    /**
     * Unique identifier for the package, combining package ID and offer ID.
     * Used for selection comparison when multiple components reference the same package with different offers.
     */
    @get:JvmSynthetic
    val packageUniqueId: String? = null,
    /**
     * The resolved offer for this package, containing the subscription option and promo offer status.
     * Used to determine offer eligibility and pricing phase information.
     */
    @get:JvmSynthetic
    val resolvedOffer: ResolvedOffer? = null,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for carousels inside tab control elements. Not for all carousels within a tab.
     */
    @get:JvmSynthetic
    val tabIndex: Int?,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedCarouselPartial>>,
) : ComponentStyle {
    @Immutable
    data class PageControlStyles(
        @get:JvmSynthetic
        val position: CarouselComponent.PageControl.Position,
        @get:JvmSynthetic
        val spacing: Dp,
        @get:JvmSynthetic
        val padding: PaddingValues,
        @get:JvmSynthetic
        val margin: PaddingValues,
        @get:JvmSynthetic
        val backgroundColor: ColorStyles?,
        @get:JvmSynthetic
        val shape: Shape,
        @get:JvmSynthetic
        val border: BorderStyles?,
        @get:JvmSynthetic
        val shadow: ShadowStyles?,
        @get:JvmSynthetic
        val active: IndicatorStyles,
        @get:JvmSynthetic
        val default: IndicatorStyles,
    )

    @Immutable
    data class IndicatorStyles(
        @get:JvmSynthetic
        val width: Dp,
        @get:JvmSynthetic
        val height: Dp,
        @get:JvmSynthetic
        val color: ColorStyles,
        @get:JvmSynthetic
        val strokeColor: ColorStyles?,
        @get:JvmSynthetic
        val strokeWidth: Dp?,
    )
}
