package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedStackPartial(
    @get:JvmSynthetic val backgroundColorStyles: ColorStyles?,
    @get:JvmSynthetic val borderStyles: BorderStyles?,
    @get:JvmSynthetic val partial: PartialStackComponent,
) : PresentedPartial<PresentedStackPartial> {

    companion object {
        /**
         * Creates a [PresentedStackPartial] from the provided [PartialStackComponent] and [aliases] map. If
         * [PartialStackComponent.backgroundColor] or [PartialStackComponent.border] is non null and contains a color
         * alias, it should exist in the [aliases] map. If it doesn't, this function will return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialStackComponent,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedStackPartial, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
            first = from.backgroundColor
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull(),
            second = from.border
                ?.toBorderStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { backgroundColorStyles, borderStyles ->
            PresentedStackPartial(
                backgroundColorStyles = backgroundColorStyles,
                borderStyles = borderStyles,
                partial = from,
            )
        }
    }

    override fun combine(with: PresentedStackPartial?): PresentedStackPartial {
        val otherPartial = with?.partial

        return PresentedStackPartial(
            backgroundColorStyles = with?.backgroundColorStyles ?: backgroundColorStyles,
            borderStyles = with?.borderStyles ?: borderStyles,
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
                badge = otherPartial?.badge ?: partial.badge,
            ),
        )
    }
}
