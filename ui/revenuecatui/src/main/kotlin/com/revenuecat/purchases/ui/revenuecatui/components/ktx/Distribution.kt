@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution

@JvmSynthetic
internal fun FlexDistribution.toHorizontalArrangement(spacing: Dp): Arrangement.Horizontal =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Start)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.End)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        FlexDistribution.SPACE_BETWEEN -> Arrangement.SpaceBetween
        FlexDistribution.SPACE_AROUND -> Arrangement.SpaceAround
        FlexDistribution.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }

@JvmSynthetic
internal fun FlexDistribution.toVerticalArrangement(spacing: Dp): Arrangement.Vertical =
    when (this) {
        FlexDistribution.START -> Arrangement.spacedBy(spacing, Alignment.Top)
        FlexDistribution.END -> Arrangement.spacedBy(spacing, Alignment.Bottom)
        FlexDistribution.CENTER -> Arrangement.spacedBy(spacing, Alignment.CenterVertically)
        FlexDistribution.SPACE_BETWEEN -> Arrangement.SpaceBetween
        FlexDistribution.SPACE_AROUND -> Arrangement.SpaceAround
        FlexDistribution.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
