package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class StickyFooterComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
) : ComponentStyle {
    override val size: Size = stackComponentStyle.size
}
