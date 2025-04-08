package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.CarouselComponent.PageControl
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
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
    val pages: List<StackComponent>,
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("initial_page_index")
    val initialPageIndex: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_alignment")
    val pageAlignment: VerticalAlignment,
    @get:JvmSynthetic
    val size: Size = Size(width = SizeConstraint.Fit, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    @SerialName("page_peek")
    val pagePeek: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_spacing")
    val pageSpacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val background: Background? = null,
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
    val autoAdvance: AutoAdvancePages? = null,
    @get:JvmSynthetic
    val overrides: List<ComponentOverride<PartialCarouselComponent>> = emptyList(),
) : PaywallComponent {

    @Poko
    @Serializable
    class AutoAdvancePages(
        @get:JvmSynthetic
        @SerialName("ms_time_per_page")
        val msTimePerPage: Int,
        @get:JvmSynthetic
        @SerialName("ms_transition_time")
        val msTransitionTime: Int,
    )

    @Poko
    @Serializable
    class PageControl(
        @get:JvmSynthetic
        val position: Position,
        @get:JvmSynthetic
        val spacing: Int? = null,
        @get:JvmSynthetic
        val padding: Padding = Padding.zero,
        @get:JvmSynthetic
        val margin: Padding = Padding.zero,
        @get:JvmSynthetic
        @SerialName("background_color")
        val backgroundColor: ColorScheme? = null,
        @get:JvmSynthetic
        val shape: Shape? = null,
        @get:JvmSynthetic
        val border: Border? = null,
        @get:JvmSynthetic
        val shadow: Shadow? = null,
        @get:JvmSynthetic
        val active: Indicator,
        @get:JvmSynthetic
        val default: Indicator,
    ) {

        @Poko
        @Serializable
        class Indicator(
            @get:JvmSynthetic
            val width: UInt,
            @get:JvmSynthetic
            val height: UInt,
            @get:JvmSynthetic
            val color: ColorScheme,
        )

        @Serializable(with = CarouselPageControlPositionDeserializer::class)
        enum class Position {
            // SerialNames are handled by the CarouselPageControlPositionDeserializer.
            TOP,
            BOTTOM,
        }
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
    @SerialName("initial_page_index")
    val initialPageIndex: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_alignment")
    val pageAlignment: VerticalAlignment? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    @SerialName("page_peek")
    val pagePeek: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_spacing")
    val pageSpacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val background: Background? = null,
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
    val autoAdvance: CarouselComponent.AutoAdvancePages? = null,
) : PartialComponent

@OptIn(InternalRevenueCatAPI::class)
internal object CarouselPageControlPositionDeserializer : EnumDeserializerWithDefault<PageControl.Position>(
    defaultValue = PageControl.Position.BOTTOM,
)
