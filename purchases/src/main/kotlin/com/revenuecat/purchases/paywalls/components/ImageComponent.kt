package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.FitMode.FIT
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("image")
internal data class ImageComponent(
    val source: ThemeImageUrls,
    val size: Size = Size(width = Fill, height = Fit),
    @SerialName("override_source_lid")
    val overrideSourceLid: LocalizationKey? = null,
    @SerialName("mask_shape")
    val maskShape: MaskShape? = null,
    @SerialName("color_overlay")
    val colorOverlay: ColorScheme? = null,
    @SerialName("fit_mode")
    val fitMode: FitMode = FIT,
    val overrides: ComponentOverrides<PartialImageComponent>? = null,
) : PaywallComponent

@Serializable
internal data class PartialImageComponent(
    val visible: Boolean? = true,
    val source: ThemeImageUrls? = null,
    val size: Size? = null,
    @SerialName("override_source_lid")
    val overrideSourceLid: LocalizationKey? = null,
    @SerialName("fit_mode")
    val fitMode: FitMode? = null,
    @SerialName("mask_shape")
    val maskShape: MaskShape? = null,
    @SerialName("color_overlay")
    val colorOverlay: ColorScheme? = null,
) : PartialComponent
