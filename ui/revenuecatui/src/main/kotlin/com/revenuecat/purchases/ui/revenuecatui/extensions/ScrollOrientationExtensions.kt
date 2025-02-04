@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.foundation.gestures.Orientation
import com.revenuecat.purchases.paywalls.components.StackComponent

@JvmSynthetic
internal fun StackComponent.ScrollOrientation.toOrientation(): Orientation = when (this) {
    StackComponent.ScrollOrientation.HORIZONTAL -> Orientation.Horizontal
    StackComponent.ScrollOrientation.VERTICAL -> Orientation.Vertical
}
