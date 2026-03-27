package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = BackgroundDeserializer::class)
public sealed interface Background {
    // SerialNames are handled by the BackgroundDeserializer

    @Serializable
    public data class Unknown(@get:JvmSynthetic val type: String) : Background

    @Serializable
    public data class Color(@get:JvmSynthetic val value: ColorScheme) : Background

    @Serializable
    public data class Image(
        @get:JvmSynthetic public val value: ThemeImageUrls,
        @get:JvmSynthetic
        @SerialName("fit_mode")
        public val fitMode: FitMode = FitMode.FILL,
        @get:JvmSynthetic
        @SerialName("color_overlay")
        public val colorOverlay: ColorScheme? = null,
    ) : Background

    @Serializable
    public data class Video(
        @get:JvmSynthetic public val value: ThemeVideoUrls,
        @get:JvmSynthetic
        @SerialName("fallback_image")
        public val fallbackImage: ThemeImageUrls,
        @get:JvmSynthetic
        val loop: Boolean,
        @get:JvmSynthetic
        @SerialName("mute_audio")
        public val muteAudio: Boolean,
        @get:JvmSynthetic
        @SerialName("fit_mode")
        public val fitMode: FitMode = FitMode.FILL,
        @get:JvmSynthetic
        @SerialName("color_overlay")
        public val colorOverlay: ColorScheme? = null,
    ) : Background
}

@OptIn(InternalRevenueCatAPI::class)
internal object BackgroundDeserializer : SealedDeserializerWithDefault<Background>(
    serialName = "Background",
    serializerByType = mapOf(
        "color" to { Background.Color.serializer() },
        "image" to { Background.Image.serializer() },
        "video" to { Background.Video.serializer() },
    ),
    defaultValue = { type -> Background.Unknown(type) },
)
