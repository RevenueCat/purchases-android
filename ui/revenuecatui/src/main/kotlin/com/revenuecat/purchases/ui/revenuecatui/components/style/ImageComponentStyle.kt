package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.Size
import java.net.URL

@Suppress("LongParameterList")
@Immutable
internal class ImageComponentStyle private constructor(
    @get:JvmSynthetic
    val visible: Boolean,
    @get:JvmSynthetic
    val url: URL,
    @get:JvmSynthetic
    val lowResURL: URL?,
    @get:JvmSynthetic
    val darkURL: URL?,
    @get:JvmSynthetic
    val darkLowResURL: URL?,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val shape: Shape?,
    @get:JvmSynthetic
    val gradientColors: ColorInfo.Gradient?,
    @get:JvmSynthetic
    val darkGradientColors: ColorInfo.Gradient?,
    @get:JvmSynthetic
    val contentScale: ContentScale,
) : ComponentStyle {

    companion object {

        @Suppress("LongParameterList")
        @JvmSynthetic
        @Composable
        operator fun invoke(
            visible: Boolean,
            url: URL,
            lowResURL: URL?,
            darkURL: URL?,
            darkLowResURL: URL?,
            size: Size,
            shape: Shape?,
            gradientColors: ColorInfo.Gradient?,
            darkGradientColors: ColorInfo.Gradient?,
            contentScale: ContentScale,
        ): ImageComponentStyle {
            return ImageComponentStyle(
                visible = visible,
                url = url,
                lowResURL = lowResURL,
                darkURL = darkURL,
                darkLowResURL = darkLowResURL,
                size = size,
                shape = shape,
                gradientColors = gradientColors,
                darkGradientColors = darkGradientColors,
                contentScale = contentScale,
            )
        }
    }

    @ReadOnlyComposable
    @JvmSynthetic
    @Composable
    fun urlToUse(): URL {
        return if (isSystemInDarkTheme()) darkURL ?: url else url
    }

    @ReadOnlyComposable
    @JvmSynthetic
    @Composable
    fun lowResUrlToUse(): URL? {
        return if (isSystemInDarkTheme()) darkLowResURL ?: lowResURL else lowResURL
    }

    @ReadOnlyComposable
    @JvmSynthetic
    @Composable
    fun gradientColors(): ColorInfo.Gradient? {
        return if (isSystemInDarkTheme()) darkGradientColors ?: gradientColors else gradientColors
    }
}
