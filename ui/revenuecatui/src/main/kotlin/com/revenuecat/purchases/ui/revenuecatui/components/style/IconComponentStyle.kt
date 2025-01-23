package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.IconComponent.Formats
import com.revenuecat.purchases.paywalls.components.IconComponent.IconBackground
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles

@Suppress("LongParameterList")
@Immutable
internal class IconComponentStyle(
    @get:JvmSynthetic
    val baseUrl: String,
    @get:JvmSynthetic
    val iconName: String,
    @get:JvmSynthetic
    val formats: Formats,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val color: ColorStyles?,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val iconBackground: IconBackground?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedIconPartial>?,
) : ComponentStyle
