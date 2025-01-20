package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.IconComponent.Formats
import com.revenuecat.purchases.paywalls.components.IconComponent.IconBackground
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("icon")
class IconComponent(
    @get:JvmSynthetic
    @SerialName("base_url")
    val baseUrl: String,
    @get:JvmSynthetic
    @SerialName("icon_name")
    val iconName: String,
    @get:JvmSynthetic
    val formats: Formats,
    @get:JvmSynthetic
    val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    val color: ColorScheme? = null,
    @get:JvmSynthetic
    val padding: Padding = zero,
    @get:JvmSynthetic
    val margin: Padding = zero,
    @get:JvmSynthetic
    @SerialName("icon_background")
    val iconBackground: IconBackground,
    @get:JvmSynthetic
    val overrides: ComponentOverrides<PartialIconComponent>? = null,
) : PaywallComponent {

    @Serializable
    class Formats(
        @get:JvmSynthetic
        val webp: String,
    )

    @Serializable
    class IconBackground(
        @get:JvmSynthetic
        val color: ColorScheme,
        @get:JvmSynthetic
        val shape: MaskShape,
        @get:JvmSynthetic
        val border: Border? = null,
        @get:JvmSynthetic
        val shadow: Shadow? = null,
    )
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PartialIconComponent(
    @get:JvmSynthetic
    val visible: Boolean? = true,
    @get:JvmSynthetic
    @SerialName("base_url")
    val baseUrl: String? = null,
    @get:JvmSynthetic
    @SerialName("icon_name")
    val iconName: String? = null,
    @get:JvmSynthetic
    val formats: Formats? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val color: ColorScheme? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
    @get:JvmSynthetic
    @SerialName("icon_background")
    val iconBackground: IconBackground? = null,
) : PartialComponent
