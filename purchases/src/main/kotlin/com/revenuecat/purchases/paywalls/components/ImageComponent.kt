package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
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
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("image")
class ImageComponent internal constructor(
    @get:JvmSynthetic
    val source: ThemeImageUrls,
    @get:JvmSynthetic
    val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    val overrideSourceLid: LocalizationKey? = null,
    @get:JvmSynthetic
    @SerialName("mask_shape")
    val maskShape: MaskShape? = null,
    @get:JvmSynthetic
    @SerialName("color_overlay")
    val colorOverlay: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("fit_mode")
    val fitMode: FitMode = FIT,
    @get:JvmSynthetic
    val overrides: ComponentOverrides<PartialImageComponent>? = null,
) : PaywallComponent

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PartialImageComponent internal constructor(
    @get:JvmSynthetic
    val visible: Boolean? = true,
    @get:JvmSynthetic
    val source: ThemeImageUrls? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    val overrideSourceLid: LocalizationKey? = null,
    @get:JvmSynthetic
    @SerialName("fit_mode")
    val fitMode: FitMode? = null,
    @get:JvmSynthetic
    @SerialName("mask_shape")
    val maskShape: MaskShape? = null,
    @get:JvmSynthetic
    @SerialName("color_overlay")
    val colorOverlay: ColorScheme? = null,
) : PartialComponent
