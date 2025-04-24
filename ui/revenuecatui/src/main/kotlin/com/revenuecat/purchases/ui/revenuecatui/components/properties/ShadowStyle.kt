package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map

/**
 * Ready to use shadow properties for the current theme.
 */
@Immutable
internal data class ShadowStyle(
    @get:JvmSynthetic val color: ColorStyle,
    @get:JvmSynthetic val radius: Dp,
    @get:JvmSynthetic val x: Dp,
    @get:JvmSynthetic val y: Dp,
)

/**
 * Shadow properties with resolved [colors].
 */
@Immutable
internal data class ShadowStyles(
    @get:JvmSynthetic val colors: ColorStyles,
    @get:JvmSynthetic val radius: Dp,
    @get:JvmSynthetic val x: Dp,
    @get:JvmSynthetic val y: Dp,
)

@JvmSynthetic
internal fun Shadow.toShadowStyles(
    aliases: Map<ColorAlias, ColorScheme>,
): Result<ShadowStyles, NonEmptyList<PaywallValidationError>> =
    color.toColorStyles(aliases)
        .map { colors ->
            ShadowStyles(
                colors = colors,
                radius = radius.dp,
                x = x.dp,
                y = y.dp,
            )
        }

@Composable
@JvmSynthetic
internal fun rememberShadowStyle(shadow: ShadowStyles): ShadowStyle {
    val colorStyle = shadow.colors.forCurrentTheme
    return remember(colorStyle) {
        ShadowStyle(
            color = colorStyle,
            radius = shadow.radius,
            x = shadow.x,
            y = shadow.y,
        )
    }
}
