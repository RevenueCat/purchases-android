package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap

@Suppress("LongParameterList")
@Immutable
internal class ImageComponentStyle(
    @get:JvmSynthetic
    val sources: NonEmptyMap<LocaleId, ThemeImageUrls>,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val shape: Shape?,
    @get:JvmSynthetic
    val overlay: ColorScheme?,
    @get:JvmSynthetic
    val contentScale: ContentScale,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedImagePartial>?,
) : ComponentStyle
