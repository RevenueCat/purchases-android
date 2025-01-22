package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedStackPartial(
    @get:JvmSynthetic val backgroundColorStyles: ColorStyles?,
    @get:JvmSynthetic val partial: PartialStackComponent,
) : PresentedPartial<PresentedStackPartial> {

    companion object {
        /**
         * Creates a [LocalizedTextPartial] from the provided [PartialTextComponent] and [LocalizationDictionary]. If
         * [PartialTextComponent.text] is non null, it should exist in the [LocalizationDictionary]. If it doesn't,
         * this function will return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialStackComponent,
            using: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedStackPartial, NonEmptyList<PaywallValidationError>> =
            from.backgroundColor
                ?.toColorStyles(aliases = using)
                .orSuccessfullyNull()
                .map { backgroundColorStyles ->
                    PresentedStackPartial(
                        backgroundColorStyles = backgroundColorStyles,
                        partial = from,
                    )
                }
    }

    override fun combine(with: PresentedStackPartial?): PresentedStackPartial {
        val otherPartial = with?.partial

        return PresentedStackPartial(
            backgroundColorStyles = with?.backgroundColorStyles ?: backgroundColorStyles,
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
