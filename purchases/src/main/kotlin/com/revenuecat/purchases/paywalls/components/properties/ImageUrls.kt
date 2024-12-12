package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.URLSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Poko
@Serializable
class ImageUrls(
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
    val width: UInt? = null,
    @get:JvmSynthetic
    val height: UInt? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ThemeImageUrls(
    @get:JvmSynthetic val light: ImageUrls,
    @get:JvmSynthetic val dark: ImageUrls? = null,
)
