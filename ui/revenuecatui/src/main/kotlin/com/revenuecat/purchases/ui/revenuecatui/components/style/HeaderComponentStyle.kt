package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class HeaderComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val firstItemIgnoresSafeArea: Boolean,
) : ComponentStyle {
    override val visible: Boolean = stackComponentStyle.visible
    override val size: Size = stackComponentStyle.size
}
