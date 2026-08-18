@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Converts this Padding to PaddingValues, interpreting all values as [dp].
 *
 * Negative values are clamped to zero. Android does not support negative padding (and uses padding
 * to apply margins too), so any negative values the dashboard sends are coerced to zero rather than
 * crashing or rendering incorrectly.
 *
 * Set [warnIfNegative] to log a warning when clamping occurs. This should only be enabled when
 * converting values straight from the dashboard model (e.g. in StyleFactory, once per paywall
 * build), not on hot recomposition paths like `derivedStateOf`, to avoid logging the same
 * misconfiguration repeatedly on state changes.
 */
@JvmSynthetic
internal fun Padding.toPaddingValues(warnIfNegative: Boolean = false): PaddingValues {
    if (warnIfNegative && minOf(top, bottom, leading, trailing) < 0.0) {
        Logger.w(
            "Received negative padding/margin value(s) " +
                "(top=$top, bottom=$bottom, leading=$leading, trailing=$trailing). " +
                "Negative padding/margin is not supported on Android; clamping to 0.",
        )
    }
    return PaddingValues(
        start = leading.coerceAtLeast(0.0).dp,
        top = top.coerceAtLeast(0.0).dp,
        end = trailing.coerceAtLeast(0.0).dp,
        bottom = bottom.coerceAtLeast(0.0).dp,
    )
}
