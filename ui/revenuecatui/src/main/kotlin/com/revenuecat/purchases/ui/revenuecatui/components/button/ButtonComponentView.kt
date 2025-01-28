@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import kotlinx.coroutines.launch

@Composable
internal fun ButtonComponentView(
    style: ButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Get a ButtonComponentState that calculates the stateful properties we should use.
    val buttonState = rememberButtonComponentState(
        style = style,
        paywallState = state,
    )

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
                onClick(buttonState.action)
                isClickable = true
            }
        },
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
                color = ColorStyles(
                    light = ColorStyle.Solid(Color.Black),
                ),
                fontSize = 15,
                fontWeight = FontWeight.REGULAR.toFontWeight(),
                fontFamily = null,
                textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                backgroundColor = ColorStyles(
                    light = ColorStyle.Solid(Color.Yellow),
                ),
                size = Size(width = Fit, height = Fit),
                padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0).toPaddingValues(),
                rcPackage = null,
                overrides = null,
            ),
        ),
        dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
        size = Size(width = Fit, height = Fit),
        spacing = 16.dp,
        backgroundColor = ColorStyles(light = ColorStyle.Solid(Color.Red)),
        padding = PaddingValues(all = 16.dp),
        margin = PaddingValues(all = 16.dp),
        shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
        border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
        shadow = ShadowStyles(
            colors = ColorStyles(ColorStyle.Solid(Color.Black)),
            radius = 10.dp,
            x = 0.dp,
            y = 3.dp,
        ),
        badge = null,
        rcPackage = null,
        overrides = null,
    ),
    action: ButtonComponentStyle.Action = ButtonComponentStyle.Action.RestorePurchases,
): ButtonComponentStyle {
    return ButtonComponentStyle(
        stackComponentStyle = stackComponentStyle,
        action = action,
    )
}
