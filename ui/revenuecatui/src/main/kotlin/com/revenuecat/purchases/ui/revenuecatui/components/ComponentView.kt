@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.button.ButtonComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.image.ImageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.PackageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter.StickyFooterComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

/**
 * A Composable that can show any [ComponentStyle].
 *
 * @param rcPackage If this Component represents a package, it should be provided here. This makes sure that any
 * variables present in TextComponent children use the values from this package, instead of from the selected package.
 */
@Suppress("LongParameterList")
@JvmSynthetic
@Composable
internal fun ComponentView(
    style: ComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    rcPackage: Package? = null,
) = when (style) {
    is StackComponentStyle -> StackComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
        selected = selected,
        rcPackage = rcPackage,
    )
    is TextComponentStyle -> TextComponentView(
        style = style,
        state = state,
        rcPackage = rcPackage,
        modifier = modifier,
        selected = selected,
    )
    is ImageComponentStyle -> ImageComponentView(style = style, state = state, modifier = modifier, selected = selected)
    is ButtonComponentStyle -> ButtonComponentView(
        style = style,
        state = state,
        onClick = onClick,
        modifier = modifier,
        selected = selected,
    )
    is StickyFooterComponentStyle -> StickyFooterComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    is PackageComponentStyle -> PackageComponentView(style = style, state = state, modifier = modifier)
}
