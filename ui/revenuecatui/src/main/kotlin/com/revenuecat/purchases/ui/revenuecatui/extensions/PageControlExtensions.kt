@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

@JvmSynthetic
internal fun CarouselComponent.PageControl.toPageControlStyles(aliases: Map<ColorAlias, ColorScheme>) =
    zipOrAccumulate(
        first = active.color.toColorStyles(aliases = aliases),
        second = default.color.toColorStyles(aliases = aliases),
        third = backgroundColor?.toColorStyles(aliases = aliases).orSuccessfullyNull(),
        fourth = border?.toBorderStyles(aliases = aliases).orSuccessfullyNull(),
        fifth = shadow?.toShadowStyles(aliases = aliases).orSuccessfullyNull(),
        sixth = active.strokeColor?.toColorStyles(aliases = aliases).orSuccessfullyNull(),
        seventh = default.strokeColor?.toColorStyles(aliases = aliases).orSuccessfullyNull(),
    ) { activeColor, defaultColor, backgroundColor, borderStyle, shadowStyle, activeStrokeColor, defaultStrokeColor ->
        CarouselComponentStyle.PageControlStyles(
            position = position,
            spacing = spacing?.dp ?: 0.dp,
            padding = padding.toPaddingValues(),
            margin = margin.toPaddingValues(),
            backgroundColor = backgroundColor,
            shape = shape ?: StyleFactory.DEFAULT_SHAPE,
            border = borderStyle,
            shadow = shadowStyle,
            active = CarouselComponentStyle.IndicatorStyles(
                width = active.width.toInt().dp,
                height = active.height.toInt().dp,
                color = activeColor,
                strokeColor = activeStrokeColor,
                strokeWidth = active.strokeWidth?.toInt()?.dp,
            ),
            default = CarouselComponentStyle.IndicatorStyles(
                width = default.width.toInt().dp,
                height = default.height.toInt().dp,
                color = defaultColor,
                strokeColor = defaultStrokeColor,
                strokeWidth = default.strokeWidth?.toInt()?.dp,
            ),
        )
    }
