@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
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
