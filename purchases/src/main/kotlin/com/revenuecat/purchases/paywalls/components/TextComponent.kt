package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
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
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("text")
class TextComponent
@Suppress("LongParameterList")
internal constructor(
    @get:JvmSynthetic
    @SerialName("text_lid")
    val text: LocalizationKey,
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("font_name")
    val fontName: String? = null,
    @get:JvmSynthetic
    @SerialName("font_weight")
    val fontWeight: FontWeight = REGULAR,
    @get:JvmSynthetic
    @SerialName("font_size")
    val fontSize: FontSize = BODY_M,
    @get:JvmSynthetic
    @SerialName("horizontal_alignment")
    val horizontalAlignment: HorizontalAlignment = CENTER,
    @get:JvmSynthetic
    val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    val padding: Padding = zero,
    @get:JvmSynthetic
    val margin: Padding = zero,
    @get:JvmSynthetic
    val overrides: ComponentOverrides<PartialTextComponent>? = null,
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
class PartialTextComponent
@Suppress("LongParameterList")
constructor(
    @get:JvmSynthetic
    val visible: Boolean? = true,
    @get:JvmSynthetic
    @SerialName("text_lid")
    val text: LocalizationKey? = null,
    @get:JvmSynthetic
    val color: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("font_name")
    val fontName: String? = null,
    @get:JvmSynthetic
    @SerialName("font_weight")
    val fontWeight: FontWeight? = null,
    @get:JvmSynthetic
    @SerialName("font_size")
    val fontSize: FontSize? = null,
    @get:JvmSynthetic
    @SerialName("horizontal_alignment")
    val horizontalAlignment: HorizontalAlignment? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
) : PartialComponent
