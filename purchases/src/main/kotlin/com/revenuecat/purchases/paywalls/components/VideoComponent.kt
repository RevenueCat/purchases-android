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
    val source: ThemeVideoUrls,
    @SerialName("fallback_source")
    @get:JvmSynthetic
    val fallbackSource: ThemeImageUrls?,
    @get:JvmSynthetic
    val visible: Boolean?,
    @SerialName("show_controls")
    @get:JvmSynthetic
    val showControls: Boolean,
    @SerialName("auto_play")
    @get:JvmSynthetic
    val autoplay: Boolean,
    @get:JvmSynthetic
    val loop: Boolean,
    @SerialName("mute_audio")
    @get:JvmSynthetic
    val muteAudio: Boolean,
    @get:JvmSynthetic
    val size: Size,
    @SerialName("fit_mode")
    @get:JvmSynthetic
    val fitMode: FitMode,
    @SerialName("mask_shape")
    @get:JvmSynthetic
    val maskShape: MaskShape?,
    @SerialName("color_overlay")
    @get:JvmSynthetic
    val colorOverlay: ColorScheme?,
    @get:JvmSynthetic
    val padding: Padding?,
    @get:JvmSynthetic
    val margin: Padding?,
    @get:JvmSynthetic
    val border: Border?,
    @get:JvmSynthetic
    val shadow: Shadow?,
    @get:JvmSynthetic
    val overrides: List<ComponentOverride<PartialVideoComponent>>?,
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    val overrideSourceLid: LocalizationKey? = null,
) : PaywallComponent

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialVideoComponent(
    @get:JvmSynthetic
    val source: ThemeVideoUrls? = null,
    @SerialName("fallback_source")
    @get:JvmSynthetic
    val fallbackSource: ThemeImageUrls? = null,
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @SerialName("show_controls")
    @get:JvmSynthetic
    val showControls: Boolean? = null,
    @SerialName("auto_play")
    @get:JvmSynthetic
    val autoplay: Boolean? = null,
    @get:JvmSynthetic
    val loop: Boolean? = null,
    @SerialName("mute_audio")
    @get:JvmSynthetic
    val muteAudio: Boolean? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @SerialName("fit_mode")
    @get:JvmSynthetic
    val fitMode: FitMode? = null,
    @SerialName("mask_shape")
    @get:JvmSynthetic
    val maskShape: MaskShape? = null,
    @SerialName("color_overlay")
    @get:JvmSynthetic
    val colorOverlay: ColorScheme? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    @SerialName("override_source_lid")
    val overrideSourceLid: LocalizationKey? = null,
) : PartialComponent
