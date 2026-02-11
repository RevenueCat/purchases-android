package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.utils.serializers.URLSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class ThemeVideoUrls(
    public @get:JvmSynthetic val light: VideoUrls,
    public @get:JvmSynthetic val dark: VideoUrls?,
)

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class VideoUrls(
    @get:JvmSynthetic
    public val width: UInt,
    @get:JvmSynthetic
    public val height: UInt,
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    public val url: URL,
    @get:JvmSynthetic
    public val checksum: Checksum? = null,
    @get:JvmSynthetic
    @SerialName("url_low_res")
    @Serializable(with = URLSerializer::class)
    public val urlLowRes: URL? = null,
    @get:JvmSynthetic
    @SerialName("checksum_low_res")
    public val checksumLowRes: Checksum? = null,
)
