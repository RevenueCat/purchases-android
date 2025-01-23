package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap

@Suppress("LongParameterList")
@Immutable
internal class ImageComponentStyle(
    @get:JvmSynthetic
    val sources: NonEmptyMap<LocaleId, ThemeImageUrls>,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val shape: Shape?,
    @get:JvmSynthetic
    val border: Border?,
    @get:JvmSynthetic
    val shadow: Shadow?,
    @get:JvmSynthetic
    val overlay: ColorStyles?,
    @get:JvmSynthetic
    val contentScale: ContentScale,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedImagePartial>?,
) : ComponentStyle
