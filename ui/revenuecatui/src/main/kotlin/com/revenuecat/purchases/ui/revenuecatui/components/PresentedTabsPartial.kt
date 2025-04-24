package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialTabsComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedTabsPartial(
    @get:JvmSynthetic val backgroundStyles: BackgroundStyles?,
    @get:JvmSynthetic val borderStyles: BorderStyles?,
    @get:JvmSynthetic val shadowStyles: ShadowStyles?,
    @get:JvmSynthetic val partial: PartialTabsComponent,
) : PresentedPartial<PresentedTabsPartial> {

    companion object {
        /**
         * Creates a [PresentedTabsPartial] from the provided [PartialTabsComponent] and [aliases] map. If
         * [PartialTabsComponent.backgroundColor], [PartialTabsComponent.border] or [PartialTabsComponent.shadow] is
         * non null and contains a color alias, it should exist in the [aliases] map. If it doesn't, this function will
         * return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialTabsComponent,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedTabsPartial, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
            first = from.background
                ?.toBackgroundStyles(aliases = aliases)
                .orSuccessfullyNull(),
            second = from.backgroundColor
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull(),
            third = from.border
                ?.toBorderStyles(aliases = aliases)
                .orSuccessfullyNull(),
            fourth = from.shadow
                ?.toShadowStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { backgroundStyles, backgroundColorStyles, borderStyles, shadowStyles ->
            PresentedTabsPartial(
                backgroundStyles = backgroundStyles ?: backgroundColorStyles?.let { BackgroundStyles.Color(it) },
                borderStyles = borderStyles,
                shadowStyles = shadowStyles,
                partial = from,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun combine(with: PresentedTabsPartial?): PresentedTabsPartial {
        val otherPartial = with?.partial

        return PresentedTabsPartial(
            backgroundStyles = with?.backgroundStyles ?: backgroundStyles,
            borderStyles = with?.borderStyles ?: borderStyles,
            shadowStyles = with?.shadowStyles ?: shadowStyles,
            partial = PartialTabsComponent(
                visible = otherPartial?.visible ?: partial.visible,
                size = otherPartial?.size ?: partial.size,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
                backgroundColor = otherPartial?.backgroundColor ?: partial.backgroundColor,
                background = otherPartial?.background ?: partial.background,
                shape = otherPartial?.shape ?: partial.shape,
                border = otherPartial?.border ?: partial.border,
                shadow = otherPartial?.shadow ?: partial.shadow,
            ),
        )
    }
}
