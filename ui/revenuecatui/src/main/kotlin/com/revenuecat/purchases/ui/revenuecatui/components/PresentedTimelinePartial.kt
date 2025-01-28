package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialTimelineComponent
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponentItem
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedTimelinePartial(
    @get:JvmSynthetic val partial: PartialTimelineComponent,
) : PresentedPartial<PresentedTimelinePartial> {
    override fun combine(with: PresentedTimelinePartial?): PresentedTimelinePartial {
        val otherPartial = with?.partial

        return PresentedTimelinePartial(
            partial = PartialTimelineComponent(
                visible = otherPartial?.visible ?: partial.visible,
                itemSpacing = otherPartial?.itemSpacing ?: partial.itemSpacing,
                textSpacing = otherPartial?.textSpacing ?: partial.textSpacing,
                columnGutter = otherPartial?.columnGutter ?: partial.columnGutter,
                iconAlignment = otherPartial?.iconAlignment ?: partial.iconAlignment,
                size = otherPartial?.size ?: partial.size,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
            ),
        )
    }
}

@Poko
internal class PresentedTimelineIconPartial(
    @get:JvmSynthetic val partial: PartialTimelineComponentItem,
) : PresentedPartial<PresentedTimelineIconPartial> {
    override fun combine(with: PresentedTimelineIconPartial?): PresentedTimelineIconPartial {
        val otherPartial = with?.partial

        return PresentedTimelineIconPartial(
            partial = PartialTimelineComponentItem(
                visible = otherPartial?.visible ?: partial.visible,
                connector = otherPartial?.connector ?: partial.connector,
            ),
        )
    }
}
