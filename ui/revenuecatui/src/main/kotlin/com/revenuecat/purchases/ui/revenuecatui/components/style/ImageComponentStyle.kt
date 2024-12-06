package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle

@Suppress("LongParameterList")
@Immutable
internal class ImageComponentStyle private constructor(
    @get:JvmSynthetic
    val visible: Boolean,
    @get:JvmSynthetic
    val themeImageUrls: ThemeImageUrls,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val shape: Shape?,
    @get:JvmSynthetic
    val overlay: ColorStyle?,
    @get:JvmSynthetic
    val contentScale: ContentScale,
) : ComponentStyle {

    companion object {

        @Suppress("LongParameterList")
        @JvmSynthetic
        @Composable
        operator fun invoke(
            visible: Boolean,
            size: Size,
            themeImageUrls: ThemeImageUrls,
            shape: Shape?,
            overlay: ColorStyle?,
            contentScale: ContentScale,
        ): ImageComponentStyle {
            return ImageComponentStyle(
                visible = visible,
                size = size,
                themeImageUrls = themeImageUrls,
                shape = shape,
                overlay = overlay,
                contentScale = contentScale,
            )
        }
    }

    @JvmSynthetic
    @ReadOnlyComposable
    @Composable
    fun adjustedSize(): Size {
        val density = LocalDensity.current
        return size.adjustForImage(imageUrls = themeImageUrls.urlsForCurrentTheme, density = density)
    }
}

private fun Size.adjustForImage(imageUrls: ImageUrls, density: Density): Size =
    Size(
        width = when (width) {
            is Fit -> Fixed(with(density) { imageUrls.width.toInt().toDp().value.toUInt() })
            is Fill,
            is Fixed,
            -> width
        },
        height = when (height) {
            is Fit -> Fixed(with(density) { imageUrls.height.toInt().toDp().value.toUInt() })
            is Fill,
            is Fixed,
            -> height
        },
    )
