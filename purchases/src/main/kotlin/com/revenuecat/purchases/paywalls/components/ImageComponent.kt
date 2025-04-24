package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.FitMode.FIT
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shadow
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
class ImageComponent(
    @get:JvmSynthetic
    val source: ThemeImageUrls,
    @get:JvmSynthetic
    val visible: Boolean? = null,
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
    val padding: Padding = zero,
    @get:JvmSynthetic
    val margin: Padding = zero,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    val overrides: List<ComponentOverride<PartialImageComponent>> = emptyList(),
) : PaywallComponent

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PartialImageComponent(
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
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
) : PartialComponent
