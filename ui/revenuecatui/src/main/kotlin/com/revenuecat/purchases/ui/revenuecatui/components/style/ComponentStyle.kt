@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

/**
 * Marker interface for component styles.
 */
@Immutable
internal sealed interface ComponentStyle {
    val visible: Boolean
    val size: Size
}
