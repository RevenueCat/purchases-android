package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.URLSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Serializable
internal data class ImageUrls internal constructor(
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    val original: URL,
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    val webp: URL,
    @get:JvmSynthetic
    @SerialName("webp_low_res")
    @Serializable(with = URLSerializer::class)
    val webpLowRes: URL,
    @get:JvmSynthetic
    val width: UInt,
    @get:JvmSynthetic
    val height: UInt,
)

@InternalRevenueCatAPI
@Serializable
internal data class ThemeImageUrls internal constructor(
    @get:JvmSynthetic val light: ImageUrls,
    @get:JvmSynthetic val dark: ImageUrls? = null,
)
