@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight as RcFontWeight

@JvmSynthetic
internal fun RcFontWeight.toFontWeight(): FontWeight =
    when (this) {
        RcFontWeight.EXTRA_LIGHT -> FontWeight.ExtraLight
        RcFontWeight.THIN -> FontWeight.Thin
        RcFontWeight.LIGHT -> FontWeight.Light
        RcFontWeight.REGULAR -> FontWeight.Normal
        RcFontWeight.MEDIUM -> FontWeight.Medium
        RcFontWeight.SEMI_BOLD -> FontWeight.SemiBold
        RcFontWeight.BOLD -> FontWeight.Bold
        RcFontWeight.EXTRA_BOLD -> FontWeight.ExtraBold
        RcFontWeight.BLACK -> FontWeight.Black
    }

@JvmSynthetic
@Composable
internal fun FontSize.toTextUnit(): TextUnit =
    with(MaterialTheme.typography) {
        // Comments contain the default values as per
        // https://m3.material.io/styles/typography/type-scale-tokens
        when (this@toTextUnit) {
            FontSize.HEADING_XXL -> displayLarge // 57
            FontSize.HEADING_XL -> displayMedium // 45
            FontSize.HEADING_L -> displaySmall // 36
            FontSize.HEADING_M -> headlineLarge // 32
            FontSize.HEADING_S -> headlineMedium // 28
            FontSize.HEADING_XS -> headlineSmall // 24
            FontSize.BODY_XL -> titleLarge // 22
            FontSize.BODY_L -> bodyLarge // 16
            FontSize.BODY_M -> bodyMedium // 14
            FontSize.BODY_S -> bodySmall // 12
        }
    }.fontSize
