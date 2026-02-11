package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
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
@Immutable
public class CarouselComponent(
    @get:JvmSynthetic
    public val pages: List<StackComponent>,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("initial_page_index")
    public val initialPageIndex: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_alignment")
    public val pageAlignment: VerticalAlignment,
    @get:JvmSynthetic
    public val size: Size = Size(width = SizeConstraint.Fit, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    @SerialName("page_peek")
    public val pagePeek: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_spacing")
    public val pageSpacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    public val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    public val background: Background? = null,
    @get:JvmSynthetic
    public val padding: Padding = Padding.zero,
    @get:JvmSynthetic
    public val margin: Padding = Padding.zero,
    @get:JvmSynthetic
    public val shape: Shape? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    @SerialName("page_control")
    public val pageControl: PageControl? = null,
    @get:JvmSynthetic
    public val loop: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("auto_advance")
    public val autoAdvance: AutoAdvancePages? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialCarouselComponent>> = emptyList(),
) : PaywallComponent {

    @Poko
    @Serializable
    class AutoAdvancePages(
        @get:JvmSynthetic
        @SerialName("ms_time_per_page")
        public val msTimePerPage: Int,
        @get:JvmSynthetic
        @SerialName("ms_transition_time")
        public val msTransitionTime: Int,
        @get:JvmSynthetic
        @SerialName("transition_type")
        public val transitionType: TransitionType?,
    ) {
        @Serializable(with = CarouselTransitionTypeDeserializer::class)
        enum class TransitionType {
            // SerialNames are handled by the CarouselTransitionTypeDeserializer.
            FADE,
            SLIDE,
        }
    }

    @Poko
    @Serializable
    @Immutable
    class PageControl(
        @get:JvmSynthetic
        public val position: Position,
        @get:JvmSynthetic
        public val spacing: Int? = null,
        @get:JvmSynthetic
        public val padding: Padding = Padding.zero,
        @get:JvmSynthetic
        public val margin: Padding = Padding.zero,
        @get:JvmSynthetic
        @SerialName("background_color")
        public val backgroundColor: ColorScheme? = null,
        @get:JvmSynthetic
        public val shape: Shape? = null,
        @get:JvmSynthetic
        public val border: Border? = null,
        @get:JvmSynthetic
        public val shadow: Shadow? = null,
        @get:JvmSynthetic
        public val active: Indicator,
        @get:JvmSynthetic
        public val default: Indicator,
    ) {

        @Poko
        @Serializable
        class Indicator(
            @get:JvmSynthetic
            public val width: UInt,
            @get:JvmSynthetic
            public val height: UInt,
            @get:JvmSynthetic
            public val color: ColorScheme,
            @get:JvmSynthetic
            @SerialName("stroke_color")
            public val strokeColor: ColorScheme? = null,
            @get:JvmSynthetic
            @SerialName("stroke_width")
            public val strokeWidth: UInt? = null,
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
@Immutable
public class PartialCarouselComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("initial_page_index")
    public val initialPageIndex: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_alignment")
    public val pageAlignment: VerticalAlignment? = null,
    @get:JvmSynthetic
    public val size: Size? = null,
    @get:JvmSynthetic
    @SerialName("page_peek")
    public val pagePeek: Int? = null,
    @get:JvmSynthetic
    @SerialName("page_spacing")
    public val pageSpacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    public val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    public val background: Background? = null,
    @get:JvmSynthetic
    public val padding: Padding? = null,
    @get:JvmSynthetic
    public val margin: Padding? = null,
    @get:JvmSynthetic
    public val shape: Shape? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    @SerialName("page_control")
    public val pageControl: PageControl? = null,
    @get:JvmSynthetic
    public val loop: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("auto_advance")
    public val autoAdvance: CarouselComponent.AutoAdvancePages? = null,
) : PartialComponent

@OptIn(InternalRevenueCatAPI::class)
internal object CarouselPageControlPositionDeserializer : EnumDeserializerWithDefault<PageControl.Position>(
    defaultValue = PageControl.Position.BOTTOM,
)

@OptIn(InternalRevenueCatAPI::class)
internal object CarouselTransitionTypeDeserializer :
    EnumDeserializerWithDefault<CarouselComponent.AutoAdvancePages.TransitionType>(
        defaultValue = CarouselComponent.AutoAdvancePages.TransitionType.SLIDE,
    )
