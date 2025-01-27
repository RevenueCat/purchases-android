package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedImagePartial(
    @get:JvmSynthetic val sources: NonEmptyMap<LocaleId, ThemeImageUrls>?,
    @get:JvmSynthetic val overlay: ColorStyles?,
    @get:JvmSynthetic val border: BorderStyles?,
    @get:JvmSynthetic val partial: PartialImageComponent,
) : PresentedPartial<PresentedImagePartial> {

    companion object {
        /**
         * Creates a [PresentedImagePartial] from the provided [PartialImageComponent] and [aliases] map. If
         * [PartialImageComponent.colorOverlay] is non null and contains a color alias, it should exist in the
         * [aliases] map. If it doesn't, this function will return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialImageComponent,
            sources: NonEmptyMap<LocaleId, ThemeImageUrls>?,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedImagePartial, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
            from.colorOverlay
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull(),
            from.border
                ?.toBorderStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { colorOverlay, border ->
            PresentedImagePartial(
                sources = sources,
                overlay = colorOverlay,
                border = border,
                partial = from,
            )
        }
    }

    override fun combine(with: PresentedImagePartial?): PresentedImagePartial {
        val otherSources = with?.sources
        val otherPartial = with?.partial

        return PresentedImagePartial(
            sources = otherSources ?: sources,
            overlay = with?.overlay ?: overlay,
            border = with?.border ?: border,
            partial = PartialImageComponent(
                visible = otherPartial?.visible ?: partial.visible,
                source = otherPartial?.source ?: partial.source,
                size = otherPartial?.size ?: partial.size,
                overrideSourceLid = otherPartial?.overrideSourceLid ?: partial.overrideSourceLid,
                fitMode = otherPartial?.fitMode ?: partial.fitMode,
                maskShape = otherPartial?.maskShape ?: partial.maskShape,
                colorOverlay = otherPartial?.colorOverlay ?: partial.colorOverlay,
            ),
        )
    }
}
