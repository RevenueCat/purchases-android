package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import dev.drewhamilton.poko.Poko

@Poko
internal class LocalizedTextPartial private constructor(
    @get:JvmSynthetic val text: String?,
    @get:JvmSynthetic val partial: PartialTextComponent,
) : PresentedPartial<LocalizedTextPartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialTextComponent,
            using: LocalizationDictionary,
        ): Result<LocalizedTextPartial> {
            val stringResult: Result<String?> = from.text?.let(using::string) ?: Result.success(null)

            return stringResult.map { string ->
                LocalizedTextPartial(
                    text = string,
                    partial = from,
                )
            }
        }
    }

    @JvmSynthetic
    override fun combine(with: LocalizedTextPartial?): LocalizedTextPartial {
        val otherPartial = with?.partial

        return LocalizedTextPartial(
            text = with?.text ?: text,
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
