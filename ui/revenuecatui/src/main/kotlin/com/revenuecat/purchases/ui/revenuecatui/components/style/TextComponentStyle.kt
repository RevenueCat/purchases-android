package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.string
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextUnit
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import java.util.Locale
import com.revenuecat.purchases.paywalls.components.properties.FontWeight as RcFontWeight

@Suppress("LongParameterList")
@Immutable
internal class TextComponentStyle private constructor(
    @get:JvmSynthetic
    val visible: Boolean,
    @get:JvmSynthetic
    val text: String,
    @get:JvmSynthetic
    val color: ColorStyle,
    @get:JvmSynthetic
    val fontSize: TextUnit,
    @get:JvmSynthetic
    val fontWeight: FontWeight?,
    @get:JvmSynthetic
    val fontFamily: FontFamily?,
    @get:JvmSynthetic
    val textAlign: TextAlign?,
    @get:JvmSynthetic
    val horizontalAlignment: Alignment.Horizontal,
    @get:JvmSynthetic
    val backgroundColor: ColorStyle?,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
) : ComponentStyle {

    companion object {

        @JvmSynthetic
        @Composable
        internal operator fun invoke(
            component: TextComponent,
            windowSize: ScreenCondition,
            isEligibleForIntroOffer: Boolean,
            componentState: ComponentViewState,
            packageContext: PackageContext,
            localizationDictionary: LocalizationDictionary,
            locale: Locale,
            variables: VariableDataProvider,
        ): Result<TextComponentStyle, List<PaywallValidationError>> = zipOrAccumulate(
            // Get our text from the localization dictionary.
            first = localizationDictionary.string(component.text).mapError { listOf(it) },
            second = component.overrides
                // Map all overrides to PresentedOverrides.
                ?.toPresentedOverrides { LocalizedTextPartial(from = it, using = localizationDictionary) }
                .orSuccessfullyNull()
                // Pick a single PresentedPartial to show.
                .map { it?.buildPresentedPartial(windowSize, isEligibleForIntroOffer, componentState) }
                .mapError { listOf(it) },
        ) { text, presentedPartial ->
            // Combine the text and PresentedPartial into a TextComponentStyle.
            val partial = presentedPartial?.partial

            TextComponentStyle(
                visible = partial?.visible ?: true,
                text = rememberProcessedText(
                    originalText = presentedPartial?.text ?: text,
                    packageContext = packageContext,
                    locale = locale,
                    variables = variables,
                ),
                color = partial?.color ?: component.color,
                fontSize = partial?.fontSize ?: component.fontSize,
                fontWeight = partial?.fontWeight ?: component.fontWeight,
                fontFamily = partial?.fontName ?: component.fontName,
                textAlign = partial?.horizontalAlignment ?: component.horizontalAlignment,
                horizontalAlignment = partial?.horizontalAlignment ?: component.horizontalAlignment,
                backgroundColor = partial?.backgroundColor ?: component.backgroundColor,
                size = partial?.size ?: component.size,
                padding = partial?.padding ?: component.padding,
                margin = partial?.margin ?: component.margin,
            )
        }

        @Suppress("LongParameterList")
        @JvmSynthetic
        @Composable
        operator fun invoke(
            visible: Boolean,
            text: String,
            color: ColorScheme,
            fontSize: FontSize,
            fontWeight: RcFontWeight?,
            fontFamily: String?,
            textAlign: HorizontalAlignment,
            horizontalAlignment: HorizontalAlignment,
            backgroundColor: ColorScheme?,
            size: Size,
            padding: Padding,
            margin: Padding,
        ): TextComponentStyle {
            val weight = fontWeight?.toFontWeight()

            return TextComponentStyle(
                visible = visible,
                text = text,
                color = color.toColorStyle(),
                fontSize = fontSize.toTextUnit(),
                fontWeight = weight,
                fontFamily = fontFamily?.let { SystemFontFamily(it, weight) },
                textAlign = textAlign.toTextAlign(),
                horizontalAlignment = horizontalAlignment.toAlignment(),
                backgroundColor = backgroundColor?.toColorStyle(),
                size = size,
                padding = padding.toPaddingValues(),
                margin = margin.toPaddingValues(),
            )
        }

        @Suppress("FunctionName")
        private fun SystemFontFamily(familyName: String, weight: FontWeight?): FontFamily =
            FontFamily(
                Font(
                    familyName = DeviceFontFamilyName(familyName),
                    weight = weight ?: FontWeight.Normal,
                ),
            )
    }
}

/**
 * Replaces any [variables] in the [originalText] with values based on the currently selected
 * [package][PackageContext.selectedPackage] and [locale].
 */
@Composable
private fun rememberProcessedText(
    originalText: String,
    packageContext: PackageContext,
    variables: VariableDataProvider,
    locale: Locale,
): String {
    val processedText by remember(packageContext) {
        derivedStateOf {
            packageContext.selectedPackage?.let { selectedPackage ->
                val discount = discountPercentage(
                    pricePerMonthMicros = selectedPackage.product.pricePerMonth()?.amountMicros,
                    mostExpensiveMicros = packageContext.variableContext.mostExpensivePricePerMonthMicros,
                )
                val variableContext: VariableProcessor.PackageContext = VariableProcessor.PackageContext(
                    discountRelativeToMostExpensivePerMonth = discount,
                    showZeroDecimalPlacePrices = packageContext.variableContext.showZeroDecimalPlacePrices,
                )
                VariableProcessor.processVariables(
                    variableDataProvider = variables,
                    context = variableContext,
                    originalString = originalText,
                    rcPackage = selectedPackage,
                    locale = locale,
                )
            } ?: originalText
        }
    }

    return processedText
}

private fun discountPercentage(pricePerMonthMicros: Long?, mostExpensiveMicros: Long?): Double? {
    if (pricePerMonthMicros == null ||
        mostExpensiveMicros == null ||
        mostExpensiveMicros <= pricePerMonthMicros
    ) {
        return null
    }

    return (mostExpensiveMicros - pricePerMonthMicros) / mostExpensiveMicros.toDouble()
}
