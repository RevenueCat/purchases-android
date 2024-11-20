package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.utils.serializers.URLSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
internal data class ImageUrls internal constructor(
    @Serializable(with = URLSerializer::class)
    val original: URL,
    @Serializable(with = URLSerializer::class)
    val webp: URL,
    @SerialName("webp_low_res")
    @Serializable(with = URLSerializer::class)
    val webpLowRes: URL,
)

@Serializable
internal data class ThemeImageUrls internal constructor(
    val light: ImageUrls,
    val dark: ImageUrls? = null,
)
