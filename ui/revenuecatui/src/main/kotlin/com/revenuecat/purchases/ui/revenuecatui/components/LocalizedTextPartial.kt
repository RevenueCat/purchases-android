package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.stringForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import dev.drewhamilton.poko.Poko

@Poko
internal class LocalizedTextPartial private constructor(
    @get:JvmSynthetic val texts: Map<LocaleId, String>?,
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
            using: Map<LocaleId, LocalizationDictionary>,
        ): Result<LocalizedTextPartial, NonEmptyList<PaywallValidationError>> =
            from.text
                ?.let { localizationKey -> using.stringForAllLocales(localizationKey) }
                .orSuccessfullyNull()
                .map { texts ->
                    LocalizedTextPartial(
                        texts = texts,
                        partial = from,
                    )
                }
    }

    @JvmSynthetic
    override fun combine(with: LocalizedTextPartial?): LocalizedTextPartial {
        val otherPartial = with?.partial

        return LocalizedTextPartial(
            texts = with?.texts ?: texts,
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
