package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.utils.serializers.URLSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Poko
@Serializable
@OptIn(InternalSerializationApi::class)
class ThemeVideoUrls(
    @get:JvmSynthetic val light: VideoUrls,
    @get:JvmSynthetic val dark: VideoUrls?,
)

@InternalRevenueCatAPI
@Poko
@Serializable
@OptIn(InternalSerializationApi::class)
class VideoUrls(
    @get:JvmSynthetic
    val width: UInt,
    @get:JvmSynthetic
    val height: UInt,
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    val url: URL,
    @get:JvmSynthetic
    val checksum: Checksum? = null,
    @get:JvmSynthetic
    @SerialName("url_low_res")
    @Serializable(with = URLSerializer::class)
    val urlLowRes: URL? = null,
    @get:JvmSynthetic
    @SerialName("checksum_low_res")
    val checksumLowRes: Checksum? = null,
)
