package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontSize.BODY_M
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.FontWeight.REGULAR
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("text")
internal data class TextComponent(
    @SerialName("text_lid")
    val text: LocalizationKey,
    val color: ColorScheme,
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @SerialName("font_name")
    val fontName: String? = null,
    @SerialName("font_weight")
    val fontWeight: FontWeight = REGULAR,
    @SerialName("font_size")
    val fontSize: FontSize = BODY_M,
    @SerialName("horizontal_alignment")
    val horizontalAlignment: HorizontalAlignment = CENTER,
    val size: Size = Size(width = Fill, height = Fit),
    val padding: Padding = zero,
    val margin: Padding = zero,
    val overrides: ComponentOverrides<PartialTextComponent>? = null,
) : PaywallComponent

@Serializable
internal data class PartialTextComponent(
    val visible: Boolean? = true,
    @SerialName("text_lid")
    val text: LocalizationKey? = null,
    val color: ColorScheme? = null,
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @SerialName("font_name")
    val fontName: String? = null,
    @SerialName("font_weight")
    val fontWeight: FontWeight? = null,
    @SerialName("font_size")
    val fontSize: FontSize? = null,
    @SerialName("horizontal_alignment")
    val horizontalAlignment: HorizontalAlignment? = null,
    val size: Size? = null,
    val padding: Padding? = null,
    val margin: Padding? = null,
) : PartialComponent
