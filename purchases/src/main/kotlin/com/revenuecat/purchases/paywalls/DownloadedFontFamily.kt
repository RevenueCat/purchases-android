package com.revenuecat.purchases.paywalls

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import dev.drewhamilton.poko.Poko
import java.io.File

@InternalRevenueCatAPI
@Poko
public class DownloadedFont(
    @get:JvmSynthetic
    public val weight: Int,
    @get:JvmSynthetic
    public val style: FontStyle,
    @get:JvmSynthetic
    public val file: File,
)

@InternalRevenueCatAPI
@Poko
public class DownloadedFontFamily(
    @get:JvmSynthetic
    public val family: String,
    @get:JvmSynthetic
    public val fonts: List<DownloadedFont> = emptyList(),
)
