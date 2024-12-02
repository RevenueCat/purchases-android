package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Ready to use border properties for the current theme.
 */
@Immutable
internal data class BorderStyle(
    @get:JvmSynthetic val width: Dp,
    @get:JvmSynthetic val color: ColorStyle,
)
