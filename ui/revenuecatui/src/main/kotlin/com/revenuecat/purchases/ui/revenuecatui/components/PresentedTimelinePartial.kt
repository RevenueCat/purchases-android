package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponent
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponentItem
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
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
    @get:JvmSynthetic val title: LocalizedTextPartial?,
    @get:JvmSynthetic val description: LocalizedTextPartial?,
    @get:JvmSynthetic val icon: PresentedIconPartial?,
    @get:JvmSynthetic val connectorStyle: TimelineComponentStyle.ConnectorStyle?,
) : PresentedPartial<PresentedTimelineItemPartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialTimelineComponentItem,
            localizations: NonEmptyMap<LocaleId, LocalizationDictionary>,
            aliases: Map<ColorAlias, ColorScheme>,
            fontAliases: Map<FontAlias, FontSpec>,
        ): Result<PresentedTimelineItemPartial, NonEmptyList<PaywallValidationError>> {
            return zipOrAccumulate(
                first = from.title
                    ?.let { partial -> LocalizedTextPartial(partial, localizations, aliases, fontAliases) }
                    .orSuccessfullyNull(),
                second = from.description
                    ?.let { partial -> LocalizedTextPartial(partial, localizations, aliases, fontAliases) }
                    .orSuccessfullyNull(),
                third = from.icon
                    ?.let { partial -> PresentedIconPartial(partial, aliases) }
                    .orSuccessfullyNull(),
                fourth = from.connector?.color?.toColorStyles(aliases = aliases).orSuccessfullyNull(),
            ) { title, description, icon, colorStyles ->
                PresentedTimelineItemPartial(
                    partial = from,
                    title = title,
                    description = description,
                    icon = icon,
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
                title = otherPartial?.title ?: partial.title,
                description = otherPartial?.description ?: partial.description,
                icon = otherPartial?.icon ?: partial.icon,
                connector = otherPartial?.connector ?: partial.connector,
            ),
            title = title.combineOrReplace(with?.title),
            description = description.combineOrReplace(with?.description),
            icon = icon.combineOrReplace(with?.icon),
            connectorStyle = with?.connectorStyle ?: connectorStyle,
        )
    }
}

private fun <T : PresentedPartial<T>> T?.combineOrReplace(with: T?): T? = this?.combine(with) ?: with
