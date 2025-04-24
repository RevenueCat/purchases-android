package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponent
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponentItem
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedTimelinePartial(
    @get:JvmSynthetic val partial: PartialTimelineComponent,
) : PresentedPartial<PresentedTimelinePartial> {
    override fun combine(with: PresentedTimelinePartial?): PresentedTimelinePartial {
        val otherPartial = with?.partial

        return PresentedTimelinePartial(
            partial = PartialTimelineComponent(
                visible = otherPartial?.visible ?: partial.visible,
                itemSpacing = otherPartial?.itemSpacing ?: partial.itemSpacing,
                textSpacing = otherPartial?.textSpacing ?: partial.textSpacing,
                columnGutter = otherPartial?.columnGutter ?: partial.columnGutter,
                iconAlignment = otherPartial?.iconAlignment ?: partial.iconAlignment,
                size = otherPartial?.size ?: partial.size,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
            ),
        )
    }
}

@Poko
internal class PresentedTimelineItemPartial(
    @get:JvmSynthetic val partial: PartialTimelineComponentItem,
    @get:JvmSynthetic val connectorStyle: TimelineComponentStyle.ConnectorStyle?,
) : PresentedPartial<PresentedTimelineItemPartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialTimelineComponentItem,
            aliases: Map<ColorAlias, ColorScheme>,
        ): Result<PresentedTimelineItemPartial, NonEmptyList<PaywallValidationError>> {
            return from.connector?.color?.toColorStyles(aliases = aliases).orSuccessfullyNull().map { colorStyles ->
                PresentedTimelineItemPartial(
                    partial = from,
                    connectorStyle = from.connector?.let { connector ->
                        if (colorStyles != null) {
                            TimelineComponentStyle.ConnectorStyle(
                                width = connector.width,
                                margin = connector.margin.toPaddingValues(),
                                color = colorStyles,
                            )
                        } else {
                            null
                        }
                    },
                )
            }
        }
    }

    override fun combine(with: PresentedTimelineItemPartial?): PresentedTimelineItemPartial {
        val otherPartial = with?.partial

        return PresentedTimelineItemPartial(
            partial = PartialTimelineComponentItem(
                visible = otherPartial?.visible ?: partial.visible,
                connector = otherPartial?.connector ?: partial.connector,
            ),
            connectorStyle = with?.connectorStyle ?: connectorStyle,
        )
    }
}
