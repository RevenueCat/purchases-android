@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import com.revenuecat.purchases.paywalls.components.properties.Size

/**
 * Marker interface for component styles.
 */
internal sealed interface ComponentStyle {
    val size: Size
}
