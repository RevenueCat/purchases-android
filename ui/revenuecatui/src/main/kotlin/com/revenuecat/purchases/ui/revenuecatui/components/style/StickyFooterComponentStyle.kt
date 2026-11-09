package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedFooterPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride

@Immutable
internal data class StickyFooterComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val layout: StickyFooterComponent.Layout = StickyFooterComponent.Layout.NORMAL,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedFooterPartial>> = emptyList(),
) : ComponentStyle {
    override val visible: Boolean = stackComponentStyle.visible
    override val size: Size = stackComponentStyle.size
}
