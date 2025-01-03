@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import kotlinx.coroutines.launch
import java.net.URL

@Composable
internal fun ButtonComponentView(
    style: ButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    var isClickable by remember { mutableStateOf(true) }
    StackComponentView(
        style = style.stackComponentStyle,
        state = state,
        // We're the button, so we're handling the click already.
        clickHandler = { },
        modifier = modifier.clickable(enabled = isClickable) {
            isClickable = false
            coroutineScope.launch {
                onClick(style.action)
                isClickable = true
            }
        },
        selected = selected,
    )
}

@Preview
@Composable
private fun ButtonComponentView_Preview_Default() {
    ButtonComponentView(previewButtonComponentStyle(), previewEmptyState(), { })
}

@Composable
private fun previewButtonComponentStyle(
    stackComponentStyle: StackComponentStyle = StackComponentStyle(
        children = listOf(
            TextComponentStyle(
                texts = nonEmptyMapOf(LocaleId("en_US") to "Restore purchases"),
                color = ColorScheme(
                    light = ColorInfo.Hex(Color.Black.toArgb()),
                ),
                fontSize = FontSize.BODY_M,
                fontWeight = FontWeight.REGULAR.toFontWeight(),
                fontFamily = null,
                textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                size = Size(width = Fit, height = Fit),
                padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0).toPaddingValues(),
                overrides = null,
            ),
        ),
        dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
        size = Size(width = Fit, height = Fit),
        spacing = 16.dp,
        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        padding = PaddingValues(all = 16.dp),
        margin = PaddingValues(all = 16.dp),
        shape = RoundedCornerShape(size = 20.dp),
        border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
        shadow = Shadow(
            color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
            radius = 10.0,
            x = 0.0,
            y = 3.0,
        ),
        badge = null,
        overrides = null,
    ),
    action: PaywallAction = PaywallAction.RestorePurchases,
): ButtonComponentStyle {
    return ButtonComponentStyle(
        stackComponentStyle = stackComponentStyle,
        action = action,
    )
}

private fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one ButtonComponent, but that's not needed for previews.
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
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    return offering.toComponentsPaywallState(validated)
}
