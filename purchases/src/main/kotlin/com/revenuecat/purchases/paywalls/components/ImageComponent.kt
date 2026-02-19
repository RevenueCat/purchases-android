package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
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
@Immutable
public class ImageComponent(
    @get:JvmSynthetic
    public val source: ThemeImageUrls,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    public val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    public val overrideSourceLid: LocalizationKey? = null,
    @get:JvmSynthetic
    @SerialName("mask_shape")
    public val maskShape: MaskShape? = null,
    @get:JvmSynthetic
    @SerialName("color_overlay")
    public val colorOverlay: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("fit_mode")
    public val fitMode: FitMode = FIT,
    @get:JvmSynthetic
    public val padding: Padding = zero,
    @get:JvmSynthetic
    public val margin: Padding = zero,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialImageComponent>> = emptyList(),
) : PaywallComponent

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialImageComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = true,
    @get:JvmSynthetic
    public val source: ThemeImageUrls? = null,
    @get:JvmSynthetic
    public val size: Size? = null,
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    public val overrideSourceLid: LocalizationKey? = null,
    @get:JvmSynthetic
    @SerialName("fit_mode")
    public val fitMode: FitMode? = null,
    @get:JvmSynthetic
    @SerialName("mask_shape")
    public val maskShape: MaskShape? = null,
    @get:JvmSynthetic
    @SerialName("color_overlay")
    public val colorOverlay: ColorScheme? = null,
    @get:JvmSynthetic
    public val padding: Padding? = null,
    @get:JvmSynthetic
    public val margin: Padding? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
) : PartialComponent
