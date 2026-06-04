package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.URLSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class ImageUrls(
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    public val original: URL,
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    public val webp: URL,
    @get:JvmSynthetic
    @SerialName("webp_low_res")
    @Serializable(with = URLSerializer::class)
    public val webpLowRes: URL,
    @get:JvmSynthetic
    public val width: UInt,
    @get:JvmSynthetic
    public val height: UInt,
)

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class ThemeImageUrls(
    @get:JvmSynthetic public val light: ImageUrls,
    @get:JvmSynthetic public val dark: ImageUrls? = null,
)
