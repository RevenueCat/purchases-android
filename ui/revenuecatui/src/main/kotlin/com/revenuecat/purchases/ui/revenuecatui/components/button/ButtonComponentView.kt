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
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle.Solid
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import kotlinx.coroutines.launch

@Composable
internal fun ButtonComponentView(
    style: ButtonComponentStyle,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var isClickable by remember { mutableStateOf(true) }
    StackComponentView(
        style.stackComponentStyle,
        modifier.clickable(enabled = isClickable) {
            isClickable = false
            coroutineScope.launch {
                style.actionHandler(style.action)
                isClickable = true
            }
        },
    )
}

@Preview
@Composable
private fun ButtonComponentView_Preview_Default() {
    ButtonComponentView(previewButtonComponentStyle())
}

@Composable
private fun previewButtonComponentStyle(
    stackComponentStyle: StackComponentStyle = StackComponentStyle(
        visible = true,
        children = listOf(
            TextComponentStyle(
                visible = true,
                text = "Restore purchases",
                color = ColorScheme(
                    light = ColorInfo.Hex(Color.Black.toArgb()),
                ),
                fontSize = FontSize.BODY_M,
                fontWeight = FontWeight.REGULAR,
                fontFamily = null,
                textAlign = HorizontalAlignment.CENTER,
                horizontalAlignment = HorizontalAlignment.CENTER,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                size = Size(width = Fit, height = Fit),
                padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
                margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0),
            ),
        ),
        dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
        size = Size(width = Fit, height = Fit),
        spacing = 16.dp,
        background = BackgroundStyle.Color(Solid(Color.Red)),
        padding = PaddingValues(all = 16.dp),
        margin = PaddingValues(all = 16.dp),
        shape = RoundedCornerShape(size = 20.dp),
        border = BorderStyle(width = 2.dp, color = Solid(Color.Blue)),
        shadow = ShadowStyle(color = Solid(Color.Black), radius = 10.dp, x = 0.dp, y = 3.dp),
    ),
    action: PaywallAction = PaywallAction.RestorePurchases,
    actionHandler: (PaywallAction) -> Unit = {},
): ButtonComponentStyle {
    return ButtonComponentStyle(
        stackComponentStyle = stackComponentStyle,
        action = action,
        actionHandler = actionHandler,
    )
}
