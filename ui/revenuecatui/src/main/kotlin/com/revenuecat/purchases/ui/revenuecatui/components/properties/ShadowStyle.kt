package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Shadow

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

@JvmSynthetic
@Composable
internal fun rememberShadowStyle(shadow: Shadow): ShadowStyle {
    val colorStyle = rememberColorStyle(shadow.color)
    return ShadowStyle(
        color = colorStyle,
        radius = shadow.radius.dp,
        x = shadow.x.dp,
        y = shadow.y.dp,
    )
}
