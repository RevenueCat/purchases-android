package com.revenuecat.purchases.ui.revenuecatui.components

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
internal fun Shadow.toShadowStyle(): ShadowStyle =
    ShadowStyle(
        color = color.toColorStyle(),
        radius = radius.dp,
        x = x.dp,
        y = y.dp,
    )
