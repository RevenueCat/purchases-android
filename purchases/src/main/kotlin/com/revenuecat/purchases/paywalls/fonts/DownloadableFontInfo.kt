package com.revenuecat.purchases.paywalls.fonts

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import com.revenuecat.purchases.utils.Result

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

@JvmSynthetic
@OptIn(InternalRevenueCatAPI::class)
internal fun FontInfo.Name.toDownloadableFontInfo(): Result<DownloadableFontInfo, String> {
    val error = when {
        url.isNullOrBlank() -> {
            "Font URL is empty for $value. Cannot download font. " +
                "Please try to re-upload your font in the RevenueCat dashboard."
        }
        hash.isNullOrBlank() -> {
            "Font hash is empty for $value. Cannot validate downloaded font. " +
                "Please try to re-upload your font in the RevenueCat dashboard."
        }
        family.isNullOrBlank() -> {
            "Font family is empty for $value. Cannot download font. " +
                "Please try to re-upload your font in the RevenueCat dashboard."
        }
        weight == null -> {
            "Font weight is null for $value. Cannot download font. " +
                "Please try to re-upload your font in the RevenueCat dashboard."
        }
        style == null -> {
            "Font style is null for $value. Cannot download font. " +
                "Please try to re-upload your font in the RevenueCat dashboard."
        }
        else -> null
    }

    return if (error != null) {
        Result.Error(error)
    } else {
        Result.Success(
            DownloadableFontInfo(
                url = url!!,
                expectedMd5 = hash!!,
                family = family!!,
                weight = weight!!,
                style = style!!,
            ),
        )
    }
}
