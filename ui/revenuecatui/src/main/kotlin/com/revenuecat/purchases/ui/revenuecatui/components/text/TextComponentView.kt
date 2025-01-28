@file:JvmSynthetic
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider

@Composable
internal fun TextComponentView(
    style: TextComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    // Get a TextComponentState that calculates the overridden properties we should use.
    val textState = rememberUpdatedTextComponentState(
        style = style,
        paywallState = state,
    )

    // Process any variables in the text.
    val context = LocalContext.current
    val variableDataProvider = remember { VariableDataProvider(context.toResourceProvider()) }
    val text = rememberProcessedText(
        state = state,
        textState = textState,
        variables = variableDataProvider,
    )

    val colorStyle = textState.color.forCurrentTheme
    val backgroundColorStyle = textState.backgroundColor?.forCurrentTheme

    // Get the text color if it's solid.
    val color = when (colorStyle) {
        is ColorStyle.Solid -> colorStyle.color
        is ColorStyle.Gradient -> Color.Unspecified
    }
    // Create a TextStyle with gradient if necessary.
    // Remove the line height, as that's not configurable anyway, so we should let Text decide the line height.
    val textStyle = when (colorStyle) {
        is ColorStyle.Solid -> LocalTextStyle.current.copy(
            lineHeight = TextUnit.Unspecified,
        )
        is ColorStyle.Gradient -> LocalTextStyle.current.copy(
            lineHeight = TextUnit.Unspecified,
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
            fontSize = textState.fontSize.sp,
            fontWeight = textState.fontWeight,
            fontFamily = textState.fontFamily,
            horizontalAlignment = textState.horizontalAlignment,
            textAlign = textState.textAlign,
            style = textStyle,
        )
    }
}

/**
 * @param fixedPackage If provided, this package will be used to take values from instead of the selected package.
 */
@Composable
private fun rememberProcessedText(
    state: PaywallState.Loaded.Components,
    textState: TextComponentState,
    variables: VariableDataProvider,
): String {
    val processedText by remember(state, textState) {
        derivedStateOf {
            textState.applicablePackage?.let { packageToUse ->

                val introEligibility = packageToUse.introEligibility

                when (introEligibility) {
                    IntroOfferEligibility.INELIGIBLE -> textState.text
                    IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE -> textState.text
                    IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE -> textState.text
                }

                val discount = discountPercentage(
                    pricePerMonthMicros = packageToUse.product.pricePerMonth()?.amountMicros,
                    mostExpensiveMicros = state.mostExpensivePricePerMonthMicros,
                )
                val variableContext: VariableProcessor.PackageContext = VariableProcessor.PackageContext(
                    discountRelativeToMostExpensivePerMonth = discount,
                    showZeroDecimalPlacePrices = !state.showPricesWithDecimals,
                )
                VariableProcessor.processVariables(
                    variableDataProvider = variables,
                    context = variableContext,
                    originalString = textState.text,
                    rcPackage = packageToUse,
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
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
        ),
        state = previewEmptyState(),
    )
}

@Preview
@Composable
private fun TextComponentView_Preview_HeadingXlExtraBold() {
    // Since we use LocalTextStyle, a MaterialTheme can influence the rendering.
    MaterialTheme {
        TextComponentView(
            style = previewTextComponentStyle(
                text = "Experience Pro today!",
                color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
                fontSize = 34,
                fontWeight = FontWeight.EXTRA_BOLD,
            ),
            state = previewEmptyState(),
        )
    }
}

@Preview(name = "SerifFont")
@Composable
private fun TextComponentView_Preview_SerifFont() {
    TextComponentView(
        style = previewTextComponentStyle(
            text = "Hello, world",
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            fontSpec = FontSpec.Generic.Serif,
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
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            fontSpec = FontSpec.Generic.SansSerif,
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
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            fontSpec = FontSpec.Generic.Monospace,
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
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            fontSpec = FontSpec.Generic.Cursive,
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
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            fontSize = 28,
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
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
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
            color = ColorStyles(light = ColorStyle.Solid(Color(red = 0xff, green = 0x00, blue = 0x00))),
            fontSize = 13,
            fontWeight = FontWeight.BLACK,
            textAlign = HorizontalAlignment.LEADING,
            horizontalAlignment = HorizontalAlignment.LEADING,
            backgroundColor = ColorStyles(light = ColorStyle.Solid(Color(red = 0xde, green = 0xde, blue = 0xde))),
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
            text = "Hello, **bold**, *italic* or _italic2_ with ~strikethrough~, ~~strikethrough2~~ and `monospace`. " +
                "Click [here](https://revenuecat.com)",
            color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
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
            color = ColorStyles(
                light = ColorInfo.Gradient.Linear(
                    degrees = -45f,
                    points = listOf(
                        ColorInfo.Gradient.Point(
                            color = Color.Cyan.toArgb(),
                            percent = 10f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0x00, green = 0x66, blue = 0xff).toArgb(),
                            percent = 30f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0xA0, green = 0x00, blue = 0xA0).toArgb(),
                            percent = 80f,
                        ),
                    ),
                ).toColorStyle(),
            ),
            fontSize = 15,
            fontWeight = FontWeight.MEDIUM,
            textAlign = HorizontalAlignment.LEADING,
            backgroundColor = ColorStyles(light = ColorStyle.Solid(Color.Black)),
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
            color = ColorStyles(
                light = ColorInfo.Gradient.Radial(
                    points = listOf(
                        ColorInfo.Gradient.Point(
                            color = Color.Cyan.toArgb(),
                            percent = 10f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0x00, green = 0x66, blue = 0xff).toArgb(),
                            percent = 80f,
                        ),
                        ColorInfo.Gradient.Point(
                            color = Color(red = 0xA0, green = 0x00, blue = 0xA0).toArgb(),
                            percent = 100f,
                        ),
                    ),
                ).toColorStyle(),
            ),
            fontSize = 15,
            fontWeight = FontWeight.MEDIUM,
            textAlign = HorizontalAlignment.LEADING,
            backgroundColor = ColorStyles(light = ColorStyle.Solid(Color.Black)),
            size = Size(width = SizeConstraint.Fixed(200.toUInt()), height = Fit),
            padding = Padding(top = 10.0, bottom = 10.0, leading = 20.0, trailing = 20.0),
            margin = Padding(top = 20.0, bottom = 20.0, leading = 10.0, trailing = 10.0),
        ),
        state = previewEmptyState(),
    )
}

@Suppress("LongParameterList")
private fun previewTextComponentStyle(
    text: String,
    color: ColorStyles,
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.REGULAR,
    fontSpec: FontSpec? = null,
    textAlign: HorizontalAlignment = HorizontalAlignment.CENTER,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    backgroundColor: ColorStyles? = null,
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
        fontSpec = fontSpec,
        textAlign = textAlign.toTextAlign(),
        horizontalAlignment = horizontalAlignment.toAlignment(),
        backgroundColor = backgroundColor,
        size = size,
        padding = padding.toPaddingValues(),
        margin = margin.toPaddingValues(),
        rcPackage = null,
        overrides = null,
    )
}
