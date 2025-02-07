package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialCarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.toPageControlStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedCarouselPartial(
    @get:JvmSynthetic val backgroundColorStyles: ColorStyles?,
    @get:JvmSynthetic val borderStyles: BorderStyles?,
    @get:JvmSynthetic val shadowStyles: ShadowStyles?,
    @get:JvmSynthetic val pageControlStyles: CarouselComponentStyle.PageControlStyles?,
    @get:JvmSynthetic val partial: PartialCarouselComponent,
) : PresentedPartial<PresentedCarouselPartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialCarouselComponent,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedCarouselPartial, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
            first = from.backgroundColor
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull(),
            second = from.border
                ?.toBorderStyles(aliases = aliases)
                .orSuccessfullyNull(),
            third = from.shadow
                ?.toShadowStyles(aliases = aliases)
                .orSuccessfullyNull(),
            fourth = from.pageControl
                ?.toPageControlStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { backgroundColor, borderStyles, shadowStyles, pageControlStyles ->
            PresentedCarouselPartial(
                backgroundColorStyles = backgroundColor,
                borderStyles = borderStyles,
                shadowStyles = shadowStyles,
                pageControlStyles = pageControlStyles,
                partial = from,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun combine(with: PresentedCarouselPartial?): PresentedCarouselPartial {
        val otherPartial = with?.partial

        return PresentedCarouselPartial(
            backgroundColorStyles = backgroundColorStyles ?: with?.backgroundColorStyles,
            borderStyles = borderStyles ?: with?.borderStyles,
            shadowStyles = shadowStyles ?: with?.shadowStyles,
            pageControlStyles = pageControlStyles ?: with?.pageControlStyles,
            partial = PartialCarouselComponent(
                visible = otherPartial?.visible ?: partial.visible,
                initialSlideIndex = otherPartial?.initialSlideIndex ?: partial.initialSlideIndex,
                alignment = otherPartial?.alignment ?: partial.alignment,
                size = otherPartial?.size ?: partial.size,
                sidePagePeek = otherPartial?.sidePagePeek ?: partial.sidePagePeek,
                spacing = otherPartial?.spacing ?: partial.spacing,
                backgroundColor = otherPartial?.backgroundColor ?: partial.backgroundColor,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
                shape = otherPartial?.shape ?: partial.shape,
                border = otherPartial?.border ?: partial.border,
                shadow = otherPartial?.shadow ?: partial.shadow,
                pageControl = otherPartial?.pageControl ?: partial.pageControl,
                loop = otherPartial?.loop ?: partial.loop,
                autoAdvance = otherPartial?.autoAdvance ?: partial.autoAdvance,
            ),
        )
    }
}
