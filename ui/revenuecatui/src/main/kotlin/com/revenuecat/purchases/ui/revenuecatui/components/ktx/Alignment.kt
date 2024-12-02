@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment

@JvmSynthetic
internal fun HorizontalAlignment.toTextAlign(): TextAlign =
    when (this) {
        HorizontalAlignment.LEADING -> TextAlign.Start
        HorizontalAlignment.CENTER -> TextAlign.Center
        HorizontalAlignment.TRAILING -> TextAlign.End
    }

@JvmSynthetic
internal fun HorizontalAlignment.toAlignment(): Alignment.Horizontal =
    when (this) {
        HorizontalAlignment.LEADING -> Alignment.Start
        HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        HorizontalAlignment.TRAILING -> Alignment.End
    }

@JvmSynthetic
internal fun VerticalAlignment.toAlignment(): Alignment.Vertical =
    when (this) {
        VerticalAlignment.TOP -> Alignment.Top
        VerticalAlignment.CENTER -> Alignment.CenterVertically
        VerticalAlignment.BOTTOM -> Alignment.Bottom
    }

@JvmSynthetic
internal fun TwoDimensionalAlignment.toAlignmentOrNull(): Alignment? =
    when (this) {
        TwoDimensionalAlignment.CENTER -> Alignment.Center
        TwoDimensionalAlignment.LEADING -> null
        TwoDimensionalAlignment.TRAILING -> null
        TwoDimensionalAlignment.TOP -> null
        TwoDimensionalAlignment.BOTTOM -> null
        TwoDimensionalAlignment.TOP_LEADING -> Alignment.TopStart
        TwoDimensionalAlignment.TOP_TRAILING -> Alignment.TopEnd
        TwoDimensionalAlignment.BOTTOM_LEADING -> Alignment.BottomStart
        TwoDimensionalAlignment.BOTTOM_TRAILING -> Alignment.BottomEnd
    }

@JvmSynthetic
internal fun TwoDimensionalAlignment.toVerticalAlignmentOrNull(): Alignment.Vertical? =
    when (this) {
        TwoDimensionalAlignment.CENTER -> Alignment.CenterVertically
        TwoDimensionalAlignment.LEADING -> null
        TwoDimensionalAlignment.TRAILING -> null
        TwoDimensionalAlignment.TOP -> Alignment.Top
        TwoDimensionalAlignment.BOTTOM -> Alignment.Bottom
        TwoDimensionalAlignment.TOP_LEADING -> Alignment.Top
        TwoDimensionalAlignment.TOP_TRAILING -> Alignment.Top
        TwoDimensionalAlignment.BOTTOM_LEADING -> Alignment.Bottom
        TwoDimensionalAlignment.BOTTOM_TRAILING -> Alignment.Bottom
    }

@JvmSynthetic
internal fun TwoDimensionalAlignment.toHorizontalAlignmentOrNull(): Alignment.Horizontal? =
    when (this) {
        TwoDimensionalAlignment.CENTER -> Alignment.CenterHorizontally
        TwoDimensionalAlignment.LEADING -> Alignment.Start
        TwoDimensionalAlignment.TRAILING -> Alignment.End
        TwoDimensionalAlignment.TOP -> null
        TwoDimensionalAlignment.BOTTOM -> null
        TwoDimensionalAlignment.TOP_LEADING -> Alignment.Start
        TwoDimensionalAlignment.TOP_TRAILING -> Alignment.End
        TwoDimensionalAlignment.BOTTOM_LEADING -> Alignment.Start
        TwoDimensionalAlignment.BOTTOM_TRAILING -> Alignment.End
    }
