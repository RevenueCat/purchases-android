@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement

/**
 * For [FlexDistribution.SPACE_AROUND] and [FlexDistribution.SPACE_EVENLY] we need to add some extra padding, as we
 * cannot use `Arrangement` to add spacing of a minimum size before or after the content. See
 * [FlexDistribution.toHorizontalArrangement] and [FlexDistribution.toVerticalArrangement] for more info.
 */
@JvmSynthetic
@Stable
internal fun Modifier.padding(dimension: Dimension, spacing: Dp): Modifier =
    when (dimension) {
        is Dimension.Horizontal -> {
            when (dimension.distribution) {
                FlexDistribution.START,
                FlexDistribution.END,
                FlexDistribution.CENTER,
                FlexDistribution.SPACE_BETWEEN,
                -> this
                FlexDistribution.SPACE_AROUND -> this.padding(horizontal = spacing / 2)
                FlexDistribution.SPACE_EVENLY -> this.padding(horizontal = spacing)
            }
        }
        is Dimension.Vertical -> when (dimension.distribution) {
            FlexDistribution.START,
            FlexDistribution.END,
            FlexDistribution.CENTER,
            FlexDistribution.SPACE_BETWEEN,
            -> this
            FlexDistribution.SPACE_AROUND -> this.padding(vertical = spacing / 2)
            FlexDistribution.SPACE_EVENLY -> this.padding(vertical = spacing)
        }
        is Dimension.ZLayer -> this
    }
