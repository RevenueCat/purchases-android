package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle

@Suppress("LongParameterList")
@Immutable
internal class ImageComponentStyle private constructor(
    @get:JvmSynthetic
    val visible: Boolean,
    @get:JvmSynthetic
    val urls: ThemeImageUrls,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val shape: Shape?,
    @get:JvmSynthetic
    val colorStyle: ColorStyle?,
    @get:JvmSynthetic
    val contentScale: ContentScale,
) : ComponentStyle {

    companion object {

        @Suppress("LongParameterList")
        @JvmSynthetic
        @Composable
        operator fun invoke(
            visible: Boolean,
            themeImageUrls: ThemeImageUrls,
            size: Size,
            shape: Shape?,
            colorStyle: ColorStyle?,
            contentScale: ContentScale,
        ): ImageComponentStyle {
            return ImageComponentStyle(
                visible = visible,
                urls = themeImageUrls,
                size = size,
                shape = shape,
                colorStyle = colorStyle,
                contentScale = contentScale,
            )
        }
    }
}
