@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed

/**
 * @param horizontalAlignment Alignment to apply when the provided [size]'s width is [Fit], and the component is
 * forced to be wider than its contents, e.g. using [widthIn] or [requiredWidth].
 * @param verticalAlignment Alignment to apply when the provided [size]'s height is [Fit], and the component is
 * forced to be taller than its contents, e.g. using [heightIn] or [requiredHeight].
 */
@JvmSynthetic
@Stable
internal fun Modifier.size(
    size: Size,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
): Modifier {
    val widthModifier = when (val width = size.width) {
        is Fit -> Modifier.wrapContentWidth(align = horizontalAlignment)
        is Fill -> Modifier.fillMaxWidth()
        is Fixed -> Modifier.width(width.value.toInt().dp)
    }

    val heightModifier = when (val height = size.height) {
        is Fit -> Modifier.wrapContentHeight(align = verticalAlignment)
        is Fill -> Modifier.fillMaxHeight()
        is Fixed -> Modifier.height(height.value.toInt().dp)
    }

    return this then widthModifier then heightModifier
}
