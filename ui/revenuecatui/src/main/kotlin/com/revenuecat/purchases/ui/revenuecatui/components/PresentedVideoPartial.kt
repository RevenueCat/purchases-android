package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialVideoComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedVideoPartial(
    @get:JvmSynthetic val sources: NonEmptyMap<LocaleId, ThemeVideoUrls>?,
    @get:JvmSynthetic val fallbackSources: NonEmptyMap<LocaleId, ThemeImageUrls>?,
    @get:JvmSynthetic val overlay: ColorStyles?,
    @get:JvmSynthetic val border: BorderStyles?,
    @get:JvmSynthetic val shadow: ShadowStyles?,
    @get:JvmSynthetic val partial: PartialVideoComponent,
) : PresentedPartial<PresentedVideoPartial> {

    companion object {
        /**
         * Creates a [PresentedVideoPartial] from the provided [PartialVideoComponent] and [aliases] map. If
         * [PartialVideoComponent.colorOverlay] is non null and contains a color alias, it should exist in the
         * [aliases] map. If it doesn't, this function will return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialVideoComponent,
            sources: NonEmptyMap<LocaleId, ThemeVideoUrls>?,
            fallbackSources: NonEmptyMap<LocaleId, ThemeImageUrls>?,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedVideoPartial, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
            first = from.colorOverlay
                ?.toColorStyles(aliases = aliases)
                .orSuccessfullyNull(),
            second = from.border
                ?.toBorderStyles(aliases = aliases)
                .orSuccessfullyNull(),
            third = from.shadow
                ?.toShadowStyles(aliases = aliases)
                .orSuccessfullyNull(),
        ) { colorOverlay, border, shadow ->
            PresentedVideoPartial(
                sources = sources,
                fallbackSources = fallbackSources,
                overlay = colorOverlay,
                border = border,
                shadow = shadow,
                partial = from,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun combine(with: PresentedVideoPartial?): PresentedVideoPartial {
        val otherSources = with?.sources
        val otherFallbackSources = with?.fallbackSources
        val otherPartial = with?.partial

        return PresentedVideoPartial(
            sources = otherSources ?: sources,
            fallbackSources = otherFallbackSources ?: fallbackSources,
            overlay = with?.overlay ?: overlay,
            border = with?.border ?: border,
            shadow = with?.shadow ?: shadow,
            partial = PartialVideoComponent(
                visible = otherPartial?.visible ?: partial.visible,
                source = otherPartial?.source ?: partial.source,
                fallbackSource = otherPartial?.fallbackSource ?: partial.fallbackSource,
                showControls = otherPartial?.showControls ?: partial.showControls,
                autoplay = otherPartial?.autoplay ?: partial.autoplay,
                loop = otherPartial?.loop ?: partial.loop,
                muteAudio = otherPartial?.muteAudio ?: partial.muteAudio,
                size = otherPartial?.size ?: partial.size,
                fitMode = otherPartial?.fitMode ?: partial.fitMode,
                maskShape = otherPartial?.maskShape ?: partial.maskShape,
                colorOverlay = otherPartial?.colorOverlay ?: partial.colorOverlay,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
                border = otherPartial?.border ?: partial.border,
                shadow = otherPartial?.shadow ?: partial.shadow,
            ),
        )
    }
}
