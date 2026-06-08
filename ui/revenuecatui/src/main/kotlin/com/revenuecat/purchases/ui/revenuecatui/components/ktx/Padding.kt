@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Padding

/**
 * Converts this Padding to PaddingValues, interpreting all values as [dp].
 *
 * Negative values are clamped to zero. Android does not support negative padding (and uses padding
 * to apply margins too), so any negative values the dashboard sends are coerced to zero rather than
 * crashing or rendering incorrectly.
 */
@JvmSynthetic
internal fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(
        start = leading.coerceAtLeast(0.0).dp,
        top = top.coerceAtLeast(0.0).dp,
        end = trailing.coerceAtLeast(0.0).dp,
        bottom = bottom.coerceAtLeast(0.0).dp,
    )
