package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.properties.Border
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
    val alignment: VerticalAlignment,
    @get:JvmSynthetic
    val size: Size = Size(width = SizeConstraint.Fit, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    val spacing: Float? = null,
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
    val loop: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("auto_advance")
    val autoAdvance: AutoAdvanceSlides? = null,
    @get:JvmSynthetic
    val overrides: ComponentOverrides<PartialCarouselComponent>? = null,
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
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PartialCarouselComponent(
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    val alignment: VerticalAlignment? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val spacing: Float? = null,
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
    val loop: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("auto_advance")
    val autoAdvance: CarouselComponent.AutoAdvanceSlides? = null,
) : PartialComponent
