package com.revenuecat.purchases.paywalls.fonts

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.paywalls.components.properties.FontStyle

@OptIn(InternalRevenueCatAPI::class)
internal data class DownloadableFontInfo(
    @get:JvmSynthetic
    val url: String,
    @get:JvmSynthetic
    val expectedMd5: String,
    @get:JvmSynthetic
    val family: String,
    @get:JvmSynthetic
    val weight: Int,
    @get:JvmSynthetic
    val style: FontStyle,
)

@Suppress("ReturnCount")
@JvmSynthetic
@OptIn(InternalRevenueCatAPI::class)
internal fun FontInfo.Name.toDownloadableFontInfo(): Result<DownloadableFontInfo> {
    if (url.isNullOrBlank()) {
        return Result.failure(
            Exception(
                "Font URL is empty for $value. Cannot download font. " +
                    "Please try to re-upload your font in the RevenueCat dashboard.",
            ),
        )
    }
    if (hash.isNullOrBlank()) {
        return Result.failure(
            Exception(
                "Font hash is empty for $value. Cannot validate downloaded font. " +
                    "Please try to re-upload your font in the RevenueCat dashboard.",
            ),
        )
    }
    if (family.isNullOrBlank()) {
        return Result.failure(
            Exception(
                "Font family is empty for $value. Cannot download font. " +
                    "Please try to re-upload your font in the RevenueCat dashboard.",
            ),
        )
    }
    if (weight == null) {
        return Result.failure(
            Exception(
                "Font weight is null for $value. Cannot download font. " +
                    "Please try to re-upload your font in the RevenueCat dashboard.",
            ),
        )
    }
    if (style == null) {
        return Result.failure(
            Exception(
                "Font style is null for $value. Cannot download font. " +
                    "Please try to re-upload your font in the RevenueCat dashboard.",
            ),
        )
    }
    return Result.success(
        DownloadableFontInfo(
            url = url,
            expectedMd5 = hash,
            family = family,
            weight = weight,
            style = style,
        ),
    )
}
