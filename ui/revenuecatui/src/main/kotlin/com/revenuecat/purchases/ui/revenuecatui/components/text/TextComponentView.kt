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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
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
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import java.net.URL

@Composable
internal fun TextComponentView(
    style: TextComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    // Get a TextComponentState that calculates the overridden properties we should use.
    val textState = rememberUpdatedTextComponentState(
        style = style,
        paywallState = state,
        selected = selected,
    )

    // Process any variables in the text.
    val context = LocalContext.current
    val variableDataProvider = remember { VariableDataProvider(context.toResourceProvider()) }
    val text = rememberProcessedText(
        state = state,
        textState = textState,
        variables = variableDataProvider,
    )

    val colorStyle = rememberColorStyle(scheme = textState.color)
    val backgroundColorStyle = textState.backgroundColor?.let { rememberColorStyle(scheme = it) }

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

    if (textState.visible) {
        Markdown(
            text = text,
            modifier = modifier
                .size(textState.size, horizontalAlignment = textState.horizontalAlignment)
                .padding(textState.margin)
                .applyIfNotNull(backgroundColorStyle) { background(it) }
                .padding(textState.padding),
            color = color,
            fontSize = textState.fontSize.toTextUnit(),
            fontWeight = textState.fontWeight,
            fontFamily = textState.fontFamily,
            horizontalAlignment = textState.horizontalAlignment,
            textAlign = textState.textAlign,
            style = textStyle,
        )
    }
}

@Composable
private fun rememberProcessedText(
    state: PaywallState.Loaded.Components,
    textState: TextComponentState,
    variables: VariableDataProvider,
): String {
    val processedText by remember(state, textState) {
        derivedStateOf {
            state.selectedPackage?.let { selectedPackage ->
                val discount = discountPercentage(
                    pricePerMonthMicros = selectedPackage.product.pricePerMonth()?.amountMicros,
                    mostExpensiveMicros = state.mostExpensivePricePerMonthMicros,
                )
                val variableContext: VariableProcessor.PackageContext = VariableProcessor.PackageContext(
                    discountRelativeToMostExpensivePerMonth = discount,
                    showZeroDecimalPlacePrices = state.showZeroDecimalPlacePrices,
                )
                VariableProcessor.processVariables(
                    variableDataProvider = variables,
                    context = variableContext,
                    originalString = textState.text,
                    rcPackage = selectedPackage,
                    locale = java.util.Locale.forLanguageTag(state.locale.toLanguageTag()),
                )
            } ?: textState.text
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
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
        state = previewEmptyState(),
    )
}

@Suppress("LongParameterList")
@Composable
private fun previewTextComponentStyle(
    text: String,
    color: ColorScheme,
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
        texts = nonEmptyMapOf(LocaleId("en_US") to text),
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
        overrides = null,
    )
}

private fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one TextComponent, but that's not needed for previews.
                stack = StackComponent(components = emptyList()),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(
                LocalizationKey("text") to LocalizationData.Text("text"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )

    return PaywallState.Loaded.Components(offering, data)
}
