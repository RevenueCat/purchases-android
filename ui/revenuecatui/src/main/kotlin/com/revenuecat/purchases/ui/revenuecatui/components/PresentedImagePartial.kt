package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedImagePartial(
    @get:JvmSynthetic val sources: NonEmptyMap<LocaleId, ThemeImageUrls>?,
    @get:JvmSynthetic val partial: PartialImageComponent,
) : PresentedPartial<PresentedImagePartial> {
    override fun combine(with: PresentedImagePartial?): PresentedImagePartial {
        val otherSources = with?.sources
        val otherPartial = with?.partial

        return PresentedImagePartial(
            sources = otherSources ?: sources,
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
