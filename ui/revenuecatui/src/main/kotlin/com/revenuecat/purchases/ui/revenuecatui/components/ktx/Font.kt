@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.ui.text.font.FontWeight
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
