package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = BackgroundDeserializer::class)
sealed interface Background {
    // SerialNames are handled by the BackgroundDeserializer

    @Serializable
    data class Unknown(@get:JvmSynthetic val type: String) : Background

    @Serializable
    data class Color(@get:JvmSynthetic val value: ColorScheme) : Background

    @Serializable
    data class Image(
        @get:JvmSynthetic val value: ThemeImageUrls,
        @get:JvmSynthetic
        @SerialName("fit_mode")
        val fitMode: FitMode = FitMode.FILL,
        @get:JvmSynthetic
        @SerialName("color_overlay")
        val colorOverlay: ColorScheme? = null,
    ) : Background
}

@OptIn(InternalRevenueCatAPI::class)
internal object BackgroundDeserializer : SealedDeserializerWithDefault<Background>(
    serialName = "Background",
    serializerByType = mapOf(
        "color" to { Background.Color.serializer() },
        "image" to { Background.Image.serializer() },
    ),
    defaultValue = { type -> Background.Unknown(type) },
)
