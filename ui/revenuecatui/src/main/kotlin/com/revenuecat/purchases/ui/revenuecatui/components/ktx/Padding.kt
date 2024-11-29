@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Padding

/**
 * Converts this Padding to PaddingValues, interpreting all values as [dp].
 */
@JvmSynthetic
internal fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(
        start = leading.dp,
        top = top.dp,
        end = trailing.dp,
        bottom = bottom.dp,
    )
