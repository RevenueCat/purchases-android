@file:JvmSynthetic
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.property.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull

@Composable
internal fun TextComponentView(
    style: TextComponentStyle,
    modifier: Modifier = Modifier,
) {
    // Get the text color if it's solid.
    val color = when (style.color) {
        is ColorStyle.Solid -> style.color.color
        is ColorStyle.Gradient -> Color.Unspecified
    }
    // Create a TextStyle with gradient if necessary.
    val textStyle = when (style.color) {
        is ColorStyle.Solid -> LocalTextStyle.current
        is ColorStyle.Gradient -> LocalTextStyle.current.copy(
            brush = style.color.brush,
        )
    }

    if (style.visible) {
        Text(
            text = style.text,
            modifier = modifier
                .size(style.size, horizontalAlignment = style.horizontalAlignment)
                .padding(style.margin)
                .applyIfNotNull(style.backgroundColor) { background(it) }
                .padding(style.padding),
            color = color,
            fontSize = style.fontSize,
            fontWeight = style.fontWeight,
            fontFamily = style.fontFamily,
            textAlign = style.textAlign,
            style = textStyle,
        )
    }
}

@Preview(name = "Default")
@Composable
private fun TextComponentView_Preview_Default() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        ),
    )
}

@Preview(name = "SerifFont")
@Composable
private fun TextComponentView_Preview_SerifFont() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontFamily = "serif",
            size = Size(width = Fit, height = Fit),
        ),
    )
}

@Preview(name = "SansSerifFont")
@Composable
private fun TextComponentView_Preview_SansSerifFont() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontFamily = "sans-serif",
            size = Size(width = Fit, height = Fit),
        ),
    )
}

@Preview(name = "MonospaceFont")
@Composable
private fun TextComponentView_Preview_MonospaceFont() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontFamily = "monospace",
            size = Size(width = Fit, height = Fit),
        ),
    )
}

@Preview(name = "CursiveFont")
@Composable
private fun TextComponentView_Preview_CursiveFont() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontFamily = "cursive",
            size = Size(width = Fit, height = Fit),
        ),
    )
}

@Preview(name = "HorizontalAlignment")
@Composable
private fun TextComponentView_Preview_HorizontalAlignment() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = Fit, height = Fit),
            horizontalAlignment = HorizontalAlignment.TRAILING,
        ),
        // Our width is Fit, but we are forced to be wider than our contents.
        modifier = Modifier.widthIn(min = 400.dp),
    )
}

@Preview(name = "Customizations")
@Composable
private fun TextComponentView_Preview_Customizations() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color(red = 0xff, green = 0x00, blue = 0x00).toArgb())),
            fontSize = FontSize.BODY_S,
            fontWeight = FontWeight.BLACK,
            textAlign = HorizontalAlignment.LEADING,
            horizontalAlignment = HorizontalAlignment.LEADING,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color(red = 0xde, green = 0xde, blue = 0xde).toArgb())),
            padding = Padding(top = 10.0, bottom = 10.0, leading = 20.0, trailing = 20.0),
            margin = Padding(top = 20.0, bottom = 20.0, leading = 10.0, trailing = 10.0),
        ),
    )
}

@Preview(name = "LinearGradient")
@Composable
private fun TextComponentView_Preview_LinearGradient() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Do not allow people to dim your shine because they are blinded. " +
                "Tell them to put some sunglasses on.",
            color = ColorScheme(
                light = ColorInfo.Gradient.Linear(
                    degrees = -45f,
                    points = listOf(
                        ColorInfo.Gradient.Point(
                            color = Color.Cyan.toArgb(),
                            percent = 0.1f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0x00, green = 0x66, blue = 0xff).toArgb(),
                            percent = 0.3f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0xA0, green = 0x00, blue = 0xA0).toArgb(),
                            percent = 0.8f,
                        ),
                    ),
                ),
            ),
            fontSize = FontSize.BODY_M,
            fontWeight = FontWeight.MEDIUM,
            textAlign = HorizontalAlignment.LEADING,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = SizeConstraint.Fixed(200.toUInt()), height = Fit),
            padding = Padding(top = 10.0, bottom = 10.0, leading = 20.0, trailing = 20.0),
            margin = Padding(top = 20.0, bottom = 20.0, leading = 10.0, trailing = 10.0),

        ),
    )
}

@Preview(name = "RadialGradient")
@Composable
private fun TextComponentView_Preview_RadialGradient() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Do not allow people to dim your shine because they are blinded. " +
                "Tell them to put some sunglasses on.",
            color = ColorScheme(
                light = ColorInfo.Gradient.Radial(
                    points = listOf(
                        ColorInfo.Gradient.Point(
                            color = Color.Cyan.toArgb(),
                            percent = 0.1f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0x00, green = 0x66, blue = 0xff).toArgb(),
                            percent = 0.8f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0xA0, green = 0x00, blue = 0xA0).toArgb(),
                            percent = 1f,
                        ),
                    ),
                ),
            ),
            fontSize = FontSize.BODY_M,
            fontWeight = FontWeight.MEDIUM,
            textAlign = HorizontalAlignment.LEADING,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = SizeConstraint.Fixed(200.toUInt()), height = Fit),
            padding = Padding(top = 10.0, bottom = 10.0, leading = 20.0, trailing = 20.0),
            margin = Padding(top = 20.0, bottom = 20.0, leading = 10.0, trailing = 10.0),

        ),
    )
}

@Suppress("LongParameterList")
@Composable
private fun previewTextComponentStyle(
    text: String,
    color: ColorScheme,
    visible: Boolean = true,
    fontSize: FontSize = FontSize.BODY_M,
    fontWeight: FontWeight = FontWeight.REGULAR,
    fontFamily: String? = null,
    textAlign: HorizontalAlignment = HorizontalAlignment.CENTER,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    backgroundColor: ColorScheme? = null,
    size: Size = Size(width = Fill, height = Fit),
    padding: Padding = zero,
    margin: Padding = zero,
) = TextComponentStyle(
    visible = visible,
    text = text,
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    textAlign = textAlign,
    horizontalAlignment = horizontalAlignment,
    backgroundColor = backgroundColor,
    size = size,
    padding = padding,
    margin = margin,
)
