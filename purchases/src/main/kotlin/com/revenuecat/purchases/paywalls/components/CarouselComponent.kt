package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.CarouselComponent.PageControl
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("carousel")
class CarouselComponent(
    @get:JvmSynthetic
    val slides: List<StackComponent>,
    @get:JvmSynthetic
    @SerialName("initial_slide_index")
    val initialSlideIndex: Int? = null,
    @get:JvmSynthetic
    val alignment: VerticalAlignment,
    @get:JvmSynthetic
    val size: Size = Size(width = SizeConstraint.Fit, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    @SerialName("side_page_peek")
    val sidePagePeek: Int? = null,
    @get:JvmSynthetic
    val spacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val padding: Padding = Padding.zero,
    @get:JvmSynthetic
    val margin: Padding = Padding.zero,
    @get:JvmSynthetic
    val shape: Shape? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    @SerialName("page_control")
    val pageControl: PageControl? = null,
    @get:JvmSynthetic
    val loop: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("auto_advance")
    val autoAdvance: AutoAdvanceSlides? = null,
    @get:JvmSynthetic
    val overrides: List<ComponentOverride<PartialCarouselComponent>> = emptyList(),
) : PaywallComponent {

    @Poko
    @Serializable
    class AutoAdvanceSlides(
        @get:JvmSynthetic
        @SerialName("ms_time_per_slide")
        val msTimePerSlide: Int,
        @get:JvmSynthetic
        @SerialName("ms_transition_time")
        val msTransitionTime: Int,
    )

    @Poko
    @Serializable
    class PageControl(
        @get:JvmSynthetic
        val alignment: VerticalAlignment,
        @get:JvmSynthetic
        val active: Indicator,
        @get:JvmSynthetic
        val default: Indicator,
    ) {

        @Poko
        @Serializable
        class Indicator(
            @get:JvmSynthetic
            val size: Size,
            @get:JvmSynthetic
            val spacing: Float? = null,
            @get:JvmSynthetic
            val color: ColorScheme,
            @get:JvmSynthetic
            val margin: Padding,
        )
    }
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PartialCarouselComponent(
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("initial_slide_index")
    val initialSlideIndex: Int? = null,
    @get:JvmSynthetic
    val alignment: VerticalAlignment? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    @SerialName("side_page_peek")
    val sidePagePeek: Int? = null,
    @get:JvmSynthetic
    val spacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
    @get:JvmSynthetic
    val shape: Shape? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    @SerialName("page_control")
    val pageControl: PageControl? = null,
    @get:JvmSynthetic
    val loop: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("auto_advance")
    val autoAdvance: CarouselComponent.AutoAdvanceSlides? = null,
) : PartialComponent
