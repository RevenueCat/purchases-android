package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialCarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyles
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
    @get:JvmSynthetic val backgroundStyles: BackgroundStyles?,
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
            fifth = from.pageControl
                ?.toPageControlStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { background, backgroundColor, borderStyles, shadowStyles, pageControlStyles ->
            PresentedCarouselPartial(
                backgroundStyles = background ?: backgroundColor?.let { BackgroundStyles.Color(it) },
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
            backgroundStyles = backgroundStyles ?: with?.backgroundStyles,
            borderStyles = borderStyles ?: with?.borderStyles,
            shadowStyles = shadowStyles ?: with?.shadowStyles,
            pageControlStyles = pageControlStyles ?: with?.pageControlStyles,
            partial = PartialCarouselComponent(
                visible = otherPartial?.visible ?: partial.visible,
                initialPageIndex = otherPartial?.initialPageIndex ?: partial.initialPageIndex,
                pageAlignment = otherPartial?.pageAlignment ?: partial.pageAlignment,
                size = otherPartial?.size ?: partial.size,
                pagePeek = otherPartial?.pagePeek ?: partial.pagePeek,
                pageSpacing = otherPartial?.pageSpacing ?: partial.pageSpacing,
                backgroundColor = otherPartial?.backgroundColor ?: partial.backgroundColor,
                background = otherPartial?.background ?: partial.background,
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
