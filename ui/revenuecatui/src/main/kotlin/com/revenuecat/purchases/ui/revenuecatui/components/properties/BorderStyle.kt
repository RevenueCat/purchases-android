package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map

/**
 * Ready to use border properties for the current theme.
 */
@Immutable
internal data class BorderStyle(
    @get:JvmSynthetic val width: Dp,
    @get:JvmSynthetic val color: ColorStyle,
)

/**
 * Border properties with resolved [colors].
 */
@Immutable
internal data class BorderStyles(
    @get:JvmSynthetic val width: Dp,
    @get:JvmSynthetic val colors: ColorStyles,
)

@JvmSynthetic
internal fun Border.toBorderStyles(
    aliases: Map<ColorAlias, ColorScheme>,
): Result<BorderStyles, NonEmptyList<PaywallValidationError>> =
    color.toColorStyles(aliases)
        .map { colors ->
            BorderStyles(
                width = width.dp,
                colors = colors,
            )
        }

@Composable
@JvmSynthetic
internal fun rememberBorderStyle(border: Border): BorderStyle {
    val colorStyle = rememberColorStyle(border.color)
    return BorderStyle(
        width = border.width.dp,
        color = colorStyle,
    )
}

@Composable
@JvmSynthetic
internal fun rememberBorderStyle(border: BorderStyles): BorderStyle {
    val colorStyle = border.colors.forCurrentTheme
    return remember(colorStyle) {
        BorderStyle(
            width = border.width,
            color = colorStyle,
        )
    }
}
