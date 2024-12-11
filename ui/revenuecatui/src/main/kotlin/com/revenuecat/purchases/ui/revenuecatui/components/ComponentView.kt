@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.button.ButtonComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.image.ImageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView

/**
 * A Composable that can show any [ComponentStyle].
 */
@JvmSynthetic
@Composable
internal fun ComponentView(
    style: ComponentStyle,
    modifier: Modifier = Modifier,
) = when (style) {
    is StackComponentStyle -> StackComponentView(style = style, modifier = modifier)
    is TextComponentStyle -> TextComponentView(style = style, modifier = modifier)
    is ImageComponentStyle -> ImageComponentView(style = style, modifier = modifier)
    is ButtonComponentStyle -> ButtonComponentView(style = style, modifier = modifier)
}
