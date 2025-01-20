package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialIconComponent
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedIconPartial(
    @get:JvmSynthetic val partial: PartialIconComponent,
) : PresentedPartial<PresentedIconPartial> {
    override fun combine(with: PresentedIconPartial?): PresentedIconPartial {
        val otherPartial = with?.partial

        return PresentedIconPartial(
            partial = PartialIconComponent(
                visible = otherPartial?.visible ?: partial.visible,
                baseUrl = otherPartial?.baseUrl ?: partial.baseUrl,
                iconName = otherPartial?.iconName ?: partial.iconName,
                formats = otherPartial?.formats ?: partial.formats,
                size = otherPartial?.size ?: partial.size,
                color = otherPartial?.color ?: partial.color,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
                iconBackground = otherPartial?.iconBackground ?: partial.iconBackground,
            ),
        )
    }
}
