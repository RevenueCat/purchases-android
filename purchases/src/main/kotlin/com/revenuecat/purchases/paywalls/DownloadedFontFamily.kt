package com.revenuecat.purchases.paywalls

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import dev.drewhamilton.poko.Poko
import java.io.File

@InternalRevenueCatAPI
@Poko
class DownloadedFont(
    @get:JvmSynthetic
    val weight: Int,
    @get:JvmSynthetic
    val style: FontStyle,
    @get:JvmSynthetic
    val file: File,
)

@InternalRevenueCatAPI
@Poko
class DownloadedFontFamily(
    @get:JvmSynthetic
    val family: String,
    @get:JvmSynthetic
    val fonts: List<DownloadedFont> = emptyList(),
)
