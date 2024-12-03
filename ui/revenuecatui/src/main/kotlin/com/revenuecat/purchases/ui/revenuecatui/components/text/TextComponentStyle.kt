package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextUnit
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.paywalls.components.properties.FontWeight as RcFontWeight

@Suppress("LongParameterList")
@Immutable
internal class TextComponentStyle private constructor(
    @JvmSynthetic
    val visible: Boolean,
    @JvmSynthetic
    val text: String,
    @JvmSynthetic
    val color: ColorStyle,
    @JvmSynthetic
    val fontSize: TextUnit,
    @JvmSynthetic
    val fontWeight: FontWeight?,
    @JvmSynthetic
    val fontFamily: FontFamily?,
    @JvmSynthetic
    val textAlign: TextAlign?,
    @JvmSynthetic
    val horizontalAlignment: Alignment.Horizontal,
    @JvmSynthetic
    val backgroundColor: ColorStyle?,
    @JvmSynthetic
    val size: Size,
    @JvmSynthetic
    val padding: PaddingValues,
    @JvmSynthetic
    val margin: PaddingValues,
) {

    companion object {

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
