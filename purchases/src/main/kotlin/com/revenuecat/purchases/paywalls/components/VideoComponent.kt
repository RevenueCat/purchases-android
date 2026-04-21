package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("video")
@Immutable
public class VideoComponent(
    @get:JvmSynthetic
    public val source: ThemeVideoUrls,
    @SerialName("fallback_source")
    @get:JvmSynthetic
    public val fallbackSource: ThemeImageUrls?,
    @get:JvmSynthetic
    public val visible: Boolean?,
    @SerialName("show_controls")
    @get:JvmSynthetic
    public val showControls: Boolean,
    @SerialName("auto_play")
    @get:JvmSynthetic
    public val autoplay: Boolean,
    @get:JvmSynthetic
    public val loop: Boolean,
    @SerialName("mute_audio")
    @get:JvmSynthetic
    public val muteAudio: Boolean,
    @get:JvmSynthetic
    public val size: Size,
    @SerialName("fit_mode")
    @get:JvmSynthetic
    public val fitMode: FitMode,
    @SerialName("mask_shape")
    @get:JvmSynthetic
    public val maskShape: MaskShape?,
    @SerialName("color_overlay")
    @get:JvmSynthetic
    public val colorOverlay: ColorScheme?,
    @get:JvmSynthetic
    public val padding: Padding?,
    @get:JvmSynthetic
    public val margin: Padding?,
    @get:JvmSynthetic
    public val border: Border?,
    @get:JvmSynthetic
    public val shadow: Shadow?,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialVideoComponent>>?,
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    public val overrideSourceLid: LocalizationKey? = null,
) : PaywallComponent

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialVideoComponent(
    @get:JvmSynthetic
    public val source: ThemeVideoUrls? = null,
    @SerialName("fallback_source")
    @get:JvmSynthetic
    public val fallbackSource: ThemeImageUrls? = null,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @SerialName("show_controls")
    @get:JvmSynthetic
    public val showControls: Boolean? = null,
    @SerialName("auto_play")
    @get:JvmSynthetic
    public val autoplay: Boolean? = null,
    @get:JvmSynthetic
    public val loop: Boolean? = null,
    @SerialName("mute_audio")
    @get:JvmSynthetic
    public val muteAudio: Boolean? = null,
    @get:JvmSynthetic
    public val size: Size? = null,
    @SerialName("fit_mode")
    @get:JvmSynthetic
    public val fitMode: FitMode? = null,
    @SerialName("mask_shape")
    @get:JvmSynthetic
    public val maskShape: MaskShape? = null,
    @SerialName("color_overlay")
    @get:JvmSynthetic
    public val colorOverlay: ColorScheme? = null,
    @get:JvmSynthetic
    public val padding: Padding? = null,
    @get:JvmSynthetic
    public val margin: Padding? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    public val overrideSourceLid: LocalizationKey? = null,
) : PartialComponent
