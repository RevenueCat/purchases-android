@file:JvmSynthetic
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
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
import com.revenuecat.purchases.ui.revenuecatui.components.SystemFontFamily
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextUnit
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider

@Composable
internal fun TextComponentView(
    style: TextComponentStyle,
    modifier: Modifier = Modifier,
    // TODO Remove these default values
    packageContext: PackageContext = PackageContext(null, PackageContext.VariableContext(emptyList())),
    locale: Locale = Locale.current,
) {
    val context = LocalContext.current
    val variableDataProvider = remember { VariableDataProvider(context.toResourceProvider()) }
    val text = rememberProcessedText(style.text, packageContext, variableDataProvider, locale)

    val colorStyle = rememberColorStyle(scheme = style.color)
    val backgroundColorStyle = style.backgroundColor?.let { rememberColorStyle(scheme = it) }

    // Get the text color if it's solid.
    val color = when (colorStyle) {
        is ColorStyle.Solid -> colorStyle.color
        is ColorStyle.Gradient -> Color.Unspecified
    }
    // Create a TextStyle with gradient if necessary.
    val textStyle = when (colorStyle) {
        is ColorStyle.Solid -> LocalTextStyle.current
        is ColorStyle.Gradient -> LocalTextStyle.current.copy(
            brush = colorStyle.brush,
        )
    }

    if (style.visible) {
        Markdown(
            text = text,
            modifier = modifier
                .size(style.size, horizontalAlignment = style.horizontalAlignment)
                .padding(style.margin)
                .applyIfNotNull(backgroundColorStyle) { background(it) }
                .padding(style.padding),
            color = color,
            fontSize = style.fontSize.toTextUnit(),
            fontWeight = style.fontWeight,
            fontFamily = style.fontFamily,
            horizontalAlignment = style.horizontalAlignment,
            textAlign = style.textAlign,
            style = textStyle,
        )
    }
}

@Composable
private fun rememberProcessedText(
    originalText: String,
    packageContext: PackageContext,
    variables: VariableDataProvider,
    locale: Locale,
): String {
    val processedText by remember(packageContext, variables, locale) {
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
                    locale = java.util.Locale.forLanguageTag(locale.toLanguageTag()),
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

@Preview(name = "Default")
@Composable
private fun TextComponentView_Preview_Default() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        ),
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
    )
}

@Preview
@Composable
private fun TextComponentView_Preview_FontSize() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontSize = FontSize.HEADING_L,
            size = Size(width = Fit, height = Fit),
        ),
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        // Our width is Fit, but we are forced to be wider than our contents.
        modifier = Modifier.widthIn(min = 400.dp),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
    )
}

@Preview(name = "Default")
@Composable
private fun TextComponentView_Preview_Markdown() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, **bold**, *italic* or _italic2_ with ~strikethrough~ and `monospace`. " +
                "Click [here](https://revenuecat.com)",
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        ),
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
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
        packageContext = previewPackageState(),
        locale = Locale.current,
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
): TextComponentStyle {
    val weight = fontWeight.toFontWeight()
    return TextComponentStyle(
        visible = visible,
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = weight,
        fontFamily = fontFamily?.let { SystemFontFamily(it, weight) },
        textAlign = textAlign.toTextAlign(),
        horizontalAlignment = horizontalAlignment.toAlignment(),
        backgroundColor = backgroundColor,
        size = size,
        padding = padding.toPaddingValues(),
        margin = margin.toPaddingValues(),
    )
}

private fun previewPackageState(): PackageContext =
    PackageContext(
        initialSelectedPackage = null,
        initialVariableContext = PackageContext.VariableContext(
            packages = emptyList(),
            showZeroDecimalPlacePrices = true,
        ),
    )
