package com.revenuecat.purchases.paywalls

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import java.io.File

@InternalRevenueCatAPI
data class DownloadedFont(
    val weight: Int,
    val style: FontStyle,
    val file: File,
)

@InternalRevenueCatAPI
data class DownloadedFontFamily(
    val family: String,
) {
    private val _fonts: MutableList<DownloadedFont> = mutableListOf()
    val fonts: List<DownloadedFont>
        get() = _fonts.toList()

    constructor(
        family: String,
        fonts: Collection<DownloadedFont>,
    ) : this(family) {
        _fonts.addAll(fonts)
    }

    internal fun addFont(font: DownloadedFont) {
        _fonts.add(font)
    }
}
