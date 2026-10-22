package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class HeaderComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    /**
     * Whether any stack in this header scrolls vertically, in its base style or in any override.
     */
    @get:JvmSynthetic
    val containsVerticalScroll: Boolean = false,
) : ComponentStyle {
    override val visible: Boolean = stackComponentStyle.visible
    override val size: Size = stackComponentStyle.size
}
