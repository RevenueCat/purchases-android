@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.foundation.gestures.Orientation
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Dimension

@JvmSynthetic
internal fun StackComponent.Overflow.toOrientation(dimension: Dimension): Orientation? = when (this) {
    StackComponent.Overflow.NONE -> null
    StackComponent.Overflow.SCROLL -> when (dimension) {
        is Dimension.Horizontal -> Orientation.Horizontal
        is Dimension.Vertical -> Orientation.Vertical
        is Dimension.ZLayer -> null
    }
}
