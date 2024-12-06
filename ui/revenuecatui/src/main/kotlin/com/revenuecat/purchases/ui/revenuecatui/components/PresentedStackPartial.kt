package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedStackPartial(
    @get:JvmSynthetic val partial: PartialStackComponent,
) : PresentedPartial<PresentedStackPartial> {
    override fun combine(with: PresentedStackPartial?): PresentedStackPartial {
        val otherPartial = with?.partial

        return PresentedStackPartial(
            partial = PartialStackComponent(
                visible = otherPartial?.visible ?: partial.visible,
                dimension = otherPartial?.dimension ?: partial.dimension,
                size = otherPartial?.size ?: partial.size,
                spacing = otherPartial?.spacing ?: partial.spacing,
                backgroundColor = otherPartial?.backgroundColor ?: partial.backgroundColor,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
                shape = otherPartial?.shape ?: partial.shape,
                border = otherPartial?.border ?: partial.border,
                shadow = otherPartial?.shadow ?: partial.shadow,
            ),
        )
    }
}
