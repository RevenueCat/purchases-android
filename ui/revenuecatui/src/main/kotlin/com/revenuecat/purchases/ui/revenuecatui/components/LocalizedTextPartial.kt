package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.stringForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.getFontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko

@Poko
internal class LocalizedTextPartial private constructor(
    @get:JvmSynthetic val texts: NonEmptyMap<LocaleId, String>?,
    @get:JvmSynthetic val color: ColorStyles?,
    @get:JvmSynthetic val backgroundColor: ColorStyles?,
    @get:JvmSynthetic val fontSpec: FontSpec?,
    @get:JvmSynthetic val partial: PartialTextComponent,
) : PresentedPartial<LocalizedTextPartial> {

    companion object {
        /**
         * Creates a [LocalizedTextPartial] from the provided [PartialTextComponent] and [LocalizationDictionary]. If
         * [PartialTextComponent.text] is non null, it should exist in the [LocalizationDictionary]. If it doesn't,
         * this function will return a failure result.
         */
        @JvmSynthetic
        operator fun invoke(
            from: PartialTextComponent,
            using: NonEmptyMap<LocaleId, LocalizationDictionary>,
            aliases: Map<ColorAlias, ColorScheme>,
            fontAliases: Map<FontAlias, FontSpec>,
        ): Result<LocalizedTextPartial, NonEmptyList<PaywallValidationError>> =
            zipOrAccumulate(
                first = from.text
                    ?.let { localizationKey -> using.stringForAllLocales(localizationKey) }
                    .orSuccessfullyNull(),
                second = from.color?.toColorStyles(aliases).orSuccessfullyNull(),
                third = from.backgroundColor?.toColorStyles(aliases).orSuccessfullyNull(),
                fourth = from.fontName
                    ?.let { fontAliases.getFontSpec(it) }
                    .orSuccessfullyNull()
                    .mapError { nonEmptyListOf(it) },
            ) { texts, color, backgroundColor, fontSpec ->
                LocalizedTextPartial(
                    texts = texts,
                    color = color,
                    backgroundColor = backgroundColor,
                    fontSpec = fontSpec,
                    partial = from,
                )
            }
    }

    @Suppress("CyclomaticComplexMethod")
    @JvmSynthetic
    override fun combine(with: LocalizedTextPartial?): LocalizedTextPartial {
        val otherPartial = with?.partial

        return LocalizedTextPartial(
            texts = with?.texts ?: texts,
            color = with?.color ?: color,
            backgroundColor = with?.backgroundColor ?: backgroundColor,
            fontSpec = with?.fontSpec ?: fontSpec,
            partial = PartialTextComponent(
                visible = otherPartial?.visible ?: partial.visible,
                text = otherPartial?.text ?: partial.text,
                color = otherPartial?.color ?: partial.color,
                backgroundColor = otherPartial?.backgroundColor ?: partial.backgroundColor,
                fontName = otherPartial?.fontName ?: partial.fontName,
                fontWeight = otherPartial?.fontWeight ?: partial.fontWeight,
                fontSize = otherPartial?.fontSize ?: partial.fontSize,
                horizontalAlignment = otherPartial?.horizontalAlignment ?: partial.horizontalAlignment,
                size = otherPartial?.size ?: partial.size,
                padding = otherPartial?.padding ?: partial.padding,
                margin = otherPartial?.margin ?: partial.margin,
            ),
        )
    }
}
