@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.button.ButtonComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.carousel.CarouselComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent.IconComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.image.ImageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.PackageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter.StickyFooterComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlToggleComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.tabs.TabControlButtonView
import com.revenuecat.purchases.ui.revenuecatui.components.tabs.TabControlToggleView
import com.revenuecat.purchases.ui.revenuecatui.components.tabs.TabsComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.timeline.TimelineComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.video.VideoComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

/**
 * A Composable that can show any [ComponentStyle].
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
@JvmSynthetic
@Composable
internal fun ComponentView(
    style: ComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) = when (style) {
    is StackComponentStyle -> StackComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    is TextComponentStyle -> TextComponentView(
        style = style,
        state = state,
        modifier = modifier,
    )
    is ImageComponentStyle -> ImageComponentView(style = style, state = state, modifier = modifier)
    is VideoComponentStyle -> VideoComponentView(
        style = style,
        state = state,
        modifier = modifier,
    )
    is ButtonComponentStyle -> ButtonComponentView(style = style, state = state, onClick = onClick, modifier = modifier)
    is StickyFooterComponentStyle -> StickyFooterComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    is PackageComponentStyle -> PackageComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    is IconComponentStyle -> IconComponentView(style = style, state = state, modifier = modifier)
    is TimelineComponentStyle -> TimelineComponentView(style = style, state = state, modifier = modifier)
    is CarouselComponentStyle -> CarouselComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    is TabsComponentStyle -> TabsComponentView(
        style = style,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    // This is a special Stack that has TabControlButtonComponentStyle children.
    is TabControlStyle.Buttons -> StackComponentView(
        style = style.stack,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    // This is a special Stack that has a TabControlToggleComponentStyle child.
    is TabControlStyle.Toggle -> StackComponentView(
        style = style.stack,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
    is TabControlButtonComponentStyle -> TabControlButtonView(
        style = style,
        state = state,
        modifier = modifier,
    )
    is TabControlToggleComponentStyle -> TabControlToggleView(
        style = style,
        state = state,
        modifier = modifier,
    )
}
