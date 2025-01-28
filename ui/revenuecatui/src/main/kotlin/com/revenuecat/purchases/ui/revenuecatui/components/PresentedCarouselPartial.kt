package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialCarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedCarouselPartial(
    @get:JvmSynthetic val borderStyles: BorderStyles?,
    @get:JvmSynthetic val shadowStyles: ShadowStyles?,
    @get:JvmSynthetic val partial: PartialCarouselComponent,
) : PresentedPartial<PresentedCarouselPartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialCarouselComponent,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedCarouselPartial, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
            first = from.border
                ?.toBorderStyles(aliases = aliases)
                .orSuccessfullyNull(),
            second = from.shadow
                ?.toShadowStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { borderStyles, shadowStyles ->
            PresentedCarouselPartial(
                borderStyles = borderStyles,
                shadowStyles = shadowStyles,
                partial = from,
            )
        }
    }

    override fun combine(with: PresentedCarouselPartial?): PresentedCarouselPartial {
        val otherPartial = with?.partial

        return PresentedCarouselPartial(
            borderStyles = borderStyles ?: with?.borderStyles,
            shadowStyles = shadowStyles ?: with?.shadowStyles,
            partial = PartialCarouselComponent(
                visible = otherPartial?.visible ?: partial.visible,
                alignment = otherPartial?.alignment ?: partial.alignment,
                size = otherPartial?.size ?: partial.size,
                spacing = otherPartial?.spacing ?: partial.spacing,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
                shape = otherPartial?.shape ?: partial.shape,
                border = otherPartial?.border ?: partial.border,
                shadow = otherPartial?.shadow ?: partial.shadow,
                loop = otherPartial?.loop ?: partial.loop,
                autoAdvance = otherPartial?.autoAdvance ?: partial.autoAdvance,
            ),
        )
    }
}
