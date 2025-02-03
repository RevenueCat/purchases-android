@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.IconComponent.Formats
import com.revenuecat.purchases.paywalls.components.IconComponent.IconBackground
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

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
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for stacks inside tab control elements. Not for all stacks within a tab.
     */
    @get:JvmSynthetic
    val tabIndex: Int?,
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
        val border: BorderStyles? = null,
        @get:JvmSynthetic
        val shadow: ShadowStyles? = null,
    )
}

@JvmSynthetic
internal fun IconBackground.toBackground(
    aliases: Map<ColorAlias, ColorScheme>,
): Result<IconComponentStyle.Background, NonEmptyList<PaywallValidationError>> =
    zipOrAccumulate(
        first = color.toColorStyles(aliases),
        second = border?.toBorderStyles(aliases).orSuccessfullyNull(),
        third = shadow?.toShadowStyles(aliases).orSuccessfullyNull(),
    ) { color, border, shadow ->
        IconComponentStyle.Background(
            color = color,
            shape = shape,
            border = border,
            shadow = shadow,
        )
    }
