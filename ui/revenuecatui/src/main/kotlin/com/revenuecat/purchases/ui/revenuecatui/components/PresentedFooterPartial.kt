package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.StickyFooterComponent

internal data class PresentedFooterPartial(
    val layout: StickyFooterComponent.Layout?,
) : PresentedPartial<PresentedFooterPartial> {
    override fun combine(with: PresentedFooterPartial?): PresentedFooterPartial =
        PresentedFooterPartial(layout = with?.layout ?: layout)
}
