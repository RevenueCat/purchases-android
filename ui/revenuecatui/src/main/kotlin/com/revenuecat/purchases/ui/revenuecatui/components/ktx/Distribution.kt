@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution

/**
 * Converts this [FlexDistribution] to an [Arrangement.Horizontal].
 *
 * Note that for [FlexDistribution.SPACE_AROUND] and [FlexDistribution.SPACE_EVENLY], you still need to add the
 * appropriate padding before and after the content manually.
 */
@JvmSynthetic
internal fun FlexDistribution.toHorizontalArrangement(spacing: Dp): Arrangement.Horizontal =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Start)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.End)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        // For these, we modify the composable tree instead by adding composables with weight(1f) where appropriate.
        FlexDistribution.SPACE_BETWEEN,
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_EVENLY,
        -> Arrangement.Start
    }

/**
 * Converts this [FlexDistribution] to an [Arrangement.Vertical].
 *
 * Note that for [FlexDistribution.SPACE_AROUND] and [FlexDistribution.SPACE_EVENLY], you still need to add the
 * appropriate padding before and after the content manually.
 */
@JvmSynthetic
internal fun FlexDistribution.toVerticalArrangement(spacing: Dp): Arrangement.Vertical =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Top)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.Bottom)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterVertically)
        // For these, we modify the composable tree instead by adding composables with weight(1f) where appropriate.
        FlexDistribution.SPACE_BETWEEN,
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_EVENLY,
        -> Arrangement.Top
    }
