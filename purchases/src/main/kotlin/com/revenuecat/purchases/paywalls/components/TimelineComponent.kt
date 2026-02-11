package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.TimelineComponent.IconAlignment
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("timeline")
@Immutable
public class TimelineComponent(
    @get:JvmSynthetic
    @SerialName("item_spacing")
    val itemSpacing: Int,
    @get:JvmSynthetic
    @SerialName("text_spacing")
    val textSpacing: Int,
    @get:JvmSynthetic
    @SerialName("column_gutter")
    val columnGutter: Int,
    @get:JvmSynthetic
    @SerialName("icon_alignment")
    val iconAlignment: IconAlignment,
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    val size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    val padding: Padding = zero,
    @get:JvmSynthetic
    val margin: Padding = zero,
    @get:JvmSynthetic
    val items: List<Item> = emptyList(),
    @get:JvmSynthetic
    val overrides: List<ComponentOverride<PartialTimelineComponent>> = emptyList(),
) : PaywallComponent {

    @Serializable(with = TimelineIconAlignmentDeserializer::class)
    enum class IconAlignment {
        // SerialNames are handled by the TimelineIconAlignmentDeserializer.

        Title,
        TitleAndDescription,
    }

    @Poko
    @Serializable
    @Immutable
    class Item(
        @get:JvmSynthetic
        public val title: TextComponent,
        @get:JvmSynthetic
        public val visible: Boolean? = null,
        @get:JvmSynthetic
        public val description: TextComponent? = null,
        @get:JvmSynthetic
        public val icon: IconComponent,
        @get:JvmSynthetic
        public val connector: Connector? = null,
        @get:JvmSynthetic
        val overrides: List<ComponentOverride<PartialTimelineComponentItem>> = emptyList(),
    )

    @Poko
    @Serializable
    @Immutable
    class Connector(
        @get:JvmSynthetic
        public val width: Int,
        @get:JvmSynthetic
        public val margin: Padding,
        @get:JvmSynthetic
        public val color: ColorScheme,
    )
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialTimelineComponent(
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    @SerialName("item_spacing")
    val itemSpacing: Int? = null,
    @get:JvmSynthetic
    @SerialName("text_spacing")
    val textSpacing: Int? = null,
    @get:JvmSynthetic
    @SerialName("column_gutter")
    val columnGutter: Int? = null,
    @get:JvmSynthetic
    @SerialName("icon_alignment")
    val iconAlignment: IconAlignment? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
) : PartialComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialTimelineComponentItem(
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    val connector: TimelineComponent.Connector? = null,
) : PartialComponent

@OptIn(InternalRevenueCatAPI::class)
internal object TimelineIconAlignmentDeserializer : EnumDeserializerWithDefault<IconAlignment>(
    defaultValue = TimelineComponent.IconAlignment.Title,
    typeForValue = { value ->
        when (value) {
            IconAlignment.Title -> "title"
            IconAlignment.TitleAndDescription -> "title_and_description"
        }
    },
)
