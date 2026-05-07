@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.button.ButtonComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.carousel.CarouselComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.CountdownComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.header.HeaderComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent.IconComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.image.ImageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.PackageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter.StickyFooterComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.HeaderComponentStyle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

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
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    val taggedModifier = (style.componentId?.let { modifier.testTag(it) } ?: modifier)
        // Surface Role.Image on image-like components so the semantics tree (used by TalkBack and
        // by layout-validation tooling) can identify them as images rather than generic containers.
        .let { tagged ->
            if (style is ImageComponentStyle || style is IconComponentStyle) {
                tagged.semantics { role = Role.Image }
            } else {
                tagged
            }
        }
    when (style) {
        is StackComponentStyle -> StackComponentView(
            style = style,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is TextComponentStyle -> TextComponentView(
            style = style,
            state = state,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is ImageComponentStyle -> ImageComponentView(style = style, state = state, modifier = taggedModifier)
        is VideoComponentStyle -> VideoComponentView(
            style = style,
            state = state,
            modifier = taggedModifier,
        )
        is ButtonComponentStyle -> ButtonComponentView(
            style = style,
            state = state,
            onClick = onClick,
            modifier = taggedModifier,
            componentInteractionTracker = componentInteractionTracker,
        )
        is HeaderComponentStyle -> HeaderComponentView(
            style = style,
            state = state,
            clickHandler = onClick,
            modifier = taggedModifier,
        )
        is StickyFooterComponentStyle -> StickyFooterComponentView(
            style = style,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is PackageComponentStyle -> PackageComponentView(
            style = style,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is IconComponentStyle -> IconComponentView(style = style, state = state, modifier = taggedModifier)
        is TimelineComponentStyle -> TimelineComponentView(style = style, state = state, modifier = taggedModifier)
        is CarouselComponentStyle -> CarouselComponentView(
            style = style,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is TabsComponentStyle -> TabsComponentView(
            style = style,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        // This is a special Stack that has TabControlButtonComponentStyle children.
        is TabControlStyle.Buttons -> StackComponentView(
            style = style.stack,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        // This is a special Stack that has a TabControlToggleComponentStyle child.
        is TabControlStyle.Toggle -> StackComponentView(
            style = style.stack,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is TabControlButtonComponentStyle -> TabControlButtonView(
            style = style,
            state = state,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is TabControlToggleComponentStyle -> TabControlToggleView(
            style = style,
            state = state,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
        is CountdownComponentStyle -> CountdownComponentView(
            style = style,
            state = state,
            onClick = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = taggedModifier,
        )
    }
}
