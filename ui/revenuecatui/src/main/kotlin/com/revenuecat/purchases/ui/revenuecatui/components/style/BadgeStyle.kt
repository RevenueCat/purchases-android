package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment

@Immutable
internal class BadgeStyle(
    @get:JvmSynthetic
    val stackStyle: StackComponentStyle,
    @get:JvmSynthetic
    val style: Badge.Style,
    @get:JvmSynthetic
    val alignment: TwoDimensionalAlignment,
)
