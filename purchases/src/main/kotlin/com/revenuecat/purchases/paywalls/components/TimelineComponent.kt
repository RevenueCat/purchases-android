package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.TimelineComponent.IconAlignment
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("timeline")
class TimelineComponent(
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

    @Serializable
    enum class IconAlignment {
        @SerialName("title")
        Title,

        @SerialName("title_and_description")
        TitleAndDescription,
    }

    @Poko
    @Serializable
    class Item(
        @get:JvmSynthetic
        val title: TextComponent,
        @get:JvmSynthetic
        val description: TextComponent? = null,
        @get:JvmSynthetic
        val icon: IconComponent,
        @get:JvmSynthetic
        val connector: Connector? = null,
        @get:JvmSynthetic
        val overrides: List<ComponentOverride<PartialTimelineComponentItem>> = emptyList(),
    )

    @Poko
    @Serializable
    class Connector(
        @get:JvmSynthetic
        val width: Int,
        @get:JvmSynthetic
        val margin: Padding,
        @get:JvmSynthetic
        val color: ColorScheme,
    )
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PartialTimelineComponent(
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
class PartialTimelineComponentItem(
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    val connector: TimelineComponent.Connector? = null,
) : PartialComponent
