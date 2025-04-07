package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = BackgroundSerializer::class)
sealed interface Background {
    @Serializable
    object Unknown : Background

    @Serializable
    @SerialName("color")
    data class Color(@get:JvmSynthetic val value: ColorScheme) : Background

    @Serializable
    @SerialName("image")
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

@InternalRevenueCatAPI
internal object BackgroundSerializer : SealedDeserializerWithDefault<Background>(
    serialName = "Background",
    serializerByType = mapOf(
        "color" to { Background.Color.serializer() },
        "image" to { Background.Image.serializer() },
    ),
    defaultSerializer = { Background.Unknown.serializer() },
)
