package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedImagePartial(
    @get:JvmSynthetic val partial: PartialImageComponent,
) : PresentedPartial<PresentedImagePartial> {
    override fun combine(with: PresentedImagePartial?): PresentedImagePartial {
        val otherPartial = with?.partial

        return PresentedImagePartial(
            partial = PartialImageComponent(
                visible = otherPartial?.visible ?: partial.visible,
                source = otherPartial?.source ?: partial.source,
                size = otherPartial?.size ?: partial.size,
                overrideSourceLid = otherPartial?.overrideSourceLid ?: partial.overrideSourceLid,
                fitMode = otherPartial?.fitMode ?: partial.fitMode,
                maskShape = otherPartial?.maskShape ?: partial.maskShape,
                colorOverlay = otherPartial?.colorOverlay ?: partial.colorOverlay,
            ),
        )
    }
}
