package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialWebViewComponent
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedWebViewPartial(
    @get:JvmSynthetic val partial: PartialWebViewComponent,
) : PresentedPartial<PresentedWebViewPartial> {
    override fun combine(with: PresentedWebViewPartial?): PresentedWebViewPartial {
        val otherPartial = with?.partial

        return PresentedWebViewPartial(
            partial = PartialWebViewComponent(
                visible = otherPartial?.visible ?: partial.visible,
            ),
        )
    }
}
