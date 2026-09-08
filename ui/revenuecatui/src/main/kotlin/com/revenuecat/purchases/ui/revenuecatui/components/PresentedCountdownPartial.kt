package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialCountdownComponent
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedCountdownPartial(
    @get:JvmSynthetic val partial: PartialCountdownComponent,
) : PresentedPartial<PresentedCountdownPartial> {
    override fun combine(with: PresentedCountdownPartial?): PresentedCountdownPartial {
        val otherPartial = with?.partial

        return PresentedCountdownPartial(
            partial = PartialCountdownComponent(
                visible = otherPartial?.visible ?: partial.visible,
            ),
        )
    }
}
