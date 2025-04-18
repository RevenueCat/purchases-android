package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialIconComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.toBackground
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedIconPartial(
    @get:JvmSynthetic val colorStyles: ColorStyles?,
    @get:JvmSynthetic val background: IconComponentStyle.Background?,
    @get:JvmSynthetic val partial: PartialIconComponent,
) : PresentedPartial<PresentedIconPartial> {

    companion object {
        /**
         * Creates a [PresentedIconPartial] from the provided [PartialIconComponent] and [aliases] map. If
         * [PartialIconComponent.color] or [PartialIconComponent.iconBackground] is non null and contains a color alias,
         * it should exist in the [aliases] map. If it doesn't, this function will return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialIconComponent,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedIconPartial, NonEmptyList<PaywallValidationError>> =
            zipOrAccumulate(
                first = from.color
                    ?.toColorStyles(aliases = aliases)
                    .orSuccessfullyNull(),
                second = from.iconBackground
                    ?.toBackground(aliases)
                    .orSuccessfullyNull(),
            ) { colorStyles, background ->
                PresentedIconPartial(
                    colorStyles = colorStyles,
                    background = background,
                    partial = from,
                )
            }
    }

    override fun combine(with: PresentedIconPartial?): PresentedIconPartial {
        val otherPartial = with?.partial

        return PresentedIconPartial(
            colorStyles = with?.colorStyles ?: colorStyles,
            background = with?.background ?: background,
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
