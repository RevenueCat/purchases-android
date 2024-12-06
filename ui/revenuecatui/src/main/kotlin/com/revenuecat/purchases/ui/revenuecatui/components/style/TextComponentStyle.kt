package com.revenuecat.purchases.ui.revenuecatui.components.style

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
    @get:JvmSynthetic
    val visible: Boolean,
    @get:JvmSynthetic
    val text: String,
    @get:JvmSynthetic
    val color: ColorScheme,
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
                color = color,
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
