@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

@JvmSynthetic
internal fun CarouselComponent.PageControl.toPageControlStyles(aliases: Map<ColorAlias, ColorScheme>) =
    zipOrAccumulate(
        first = active.color.toColorStyles(aliases = aliases),
        second = default.color.toColorStyles(aliases = aliases),
    ) { activeColor, defaultColor ->
        CarouselComponentStyle.PageControlStyles(
            alignment = alignment.toAlignment(),
            active = CarouselComponentStyle.IndicatorStyles(
                size = active.size,
                spacing = active.spacing?.dp ?: 0.dp,
                color = activeColor,
                margin = active.margin.toPaddingValues(),
            ),
            default = CarouselComponentStyle.IndicatorStyles(
                size = default.size,
                spacing = default.spacing?.dp ?: 0.dp,
                color = defaultColor,
                margin = default.margin.toPaddingValues(),
            ),
        )
    }
