package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Border

/**
 * Ready to use border properties for the current theme.
 */
@Immutable
internal data class BorderStyle(
    @get:JvmSynthetic val width: Dp,
    @get:JvmSynthetic val color: ColorStyle,
)

@Composable
@JvmSynthetic
internal fun Border.toBorderStyle(): BorderStyle =
    BorderStyle(
        width = width.dp,
        color = color.toColorStyle(),
    )
