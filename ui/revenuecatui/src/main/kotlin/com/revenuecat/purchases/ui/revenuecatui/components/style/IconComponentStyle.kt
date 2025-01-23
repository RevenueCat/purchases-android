@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.IconComponent.Formats
import com.revenuecat.purchases.paywalls.components.IconComponent.IconBackground
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map

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
    val iconBackground: Background?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedIconPartial>?,
) : ComponentStyle {
    @Immutable
    internal class Background(
        @get:JvmSynthetic
        val color: ColorStyles,
        @get:JvmSynthetic
        val shape: MaskShape,
        @get:JvmSynthetic
        val border: Border? = null,
        @get:JvmSynthetic
        val shadow: Shadow? = null,
    )
}

@JvmSynthetic
internal fun IconBackground.toBackground(
    aliases: Map<ColorAlias, ColorScheme>,
): Result<IconComponentStyle.Background, NonEmptyList<PaywallValidationError>> =
    color.toColorStyles(aliases).map { colorStyles ->
        IconComponentStyle.Background(
            color = colorStyles,
            shape = shape,
            border = border,
            shadow = shadow,
        )
    }
