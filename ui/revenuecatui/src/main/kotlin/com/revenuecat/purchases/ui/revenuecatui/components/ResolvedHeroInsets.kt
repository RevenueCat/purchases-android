@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.button.rememberUpdatedButtonVisibilityState
import com.revenuecat.purchases.ui.revenuecatui.components.carousel.rememberUpdatedCarouselComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.rememberActiveCountdownStack
import com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent.rememberUpdatedIconComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.image.rememberUpdatedImageComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.rememberUpdatedPackageComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.stack.rememberUpdatedStackComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.tabs.rememberUpdatedTabsComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.text.rememberUpdatedTextComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.timeline.rememberUpdatedTimelineComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.video.rememberUpdatedVideoComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.webview.rememberUpdatedWebViewComponentState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

/** Style data classes can be equal while belonging to different rendered trees. */
internal class ComponentStyleIdentity(private val style: ComponentStyle) {
    override fun equals(other: Any?): Boolean = other is ComponentStyleIdentity && style === other.style
    override fun hashCode(): Int = System.identityHashCode(style)
}

internal fun PaywallState.Loaded.Components.heroInsetsFor(style: ComponentStyle): ResolvedHeroInsets? =
    resolvedHeroInsets[ComponentStyleIdentity(style)]

internal data class ResolvedHeroInsets(
    val root: StackComponentStyle,
    val hero: ComponentStyle?,
    val owner: StackComponentStyle?,
    val firstVisibleChildren: List<Pair<StackComponentStyle, ComponentStyle>>,
    private val identities: List<ComponentStyleIdentity> =
        (listOfNotNull(root, hero, owner) + firstVisibleChildren.flatMap { listOf(it.first, it.second) })
            .map(::ComponentStyleIdentity),
) {
    fun firstVisibleChildFor(stack: StackComponentStyle): ComponentStyle? =
        firstVisibleChildren.firstOrNull { it.first === stack }?.second

    fun appliesTo(stack: StackComponentStyle): Boolean =
        stack === owner || (stack === root && root.windowInsetsRoot == true && owner == null)
}

private data class FirstVisibleLeaf(
    val style: ComponentStyle,
    val fullWidthMedia: Boolean,
    val ancestors: List<Pair<StackComponentStyle, ComponentStyle>> = emptyList(),
)

/** Resolve after MeasurePaywallBounds, using the same visibility and size states as rendering. */
@Composable
internal fun resolveHeroInsets(
    root: ComponentStyle,
    state: PaywallState.Loaded.Components,
): ResolvedHeroInsets? {
    if (root !is StackComponentStyle) return null
    val leaf = firstVisibleLeaf(root, state)
    val firstChildren = leaf?.ancestors.orEmpty()
    val hero = leaf?.takeIf { it.fullWidthMedia }?.style
    var owner: StackComponentStyle? = null
    for ((stack, child) in firstChildren) {
        if (child === hero && rememberDimensionIsZLayer(stack, state)) owner = stack
    }
    val insets = ResolvedHeroInsets(root, hero, owner, firstChildren)
    val entries = mutableMapOf<ComponentStyleIdentity, ResolvedHeroInsets>()
    recordInsets(root, insets, entries)
    state.resolvedHeroInsets = state.resolvedHeroInsets + entries
    if (root === state.stack) state.mainStackHasHeroImage = hero != null
    return insets
}

private fun recordInsets(
    style: ComponentStyle,
    insets: ResolvedHeroInsets,
    entries: MutableMap<ComponentStyleIdentity, ResolvedHeroInsets>,
) {
    entries[ComponentStyleIdentity(style)] = insets
    // Wrapper stacks render in this same tree and must not fall back to stale factory inset hints.
    val children = when (style) {
        is StackComponentStyle -> style.children + listOfNotNull(style.badge?.stackStyle) +
            style.overrides.mapNotNull { it.properties.badgeStyle?.stackStyle }
        is ButtonComponentStyle -> listOf(style.stackComponentStyle)
        is PackageComponentStyle -> listOf(style.stackComponentStyle)
        is CountdownComponentStyle -> listOfNotNull(
            style.countdownStackComponentStyle,
            style.endStackComponentStyle,
            style.fallbackStackComponentStyle,
        )
        is CarouselComponentStyle -> style.pages
        is TabsComponentStyle -> listOf(style.control) + style.tabs.map { it.stack }
        is TabControlButtonComponentStyle -> listOf(style.stack)
        is TabControlStyle.Buttons -> listOf(style.stack)
        is TabControlStyle.Toggle -> listOf(style.stack)
        else -> emptyList()
    }
    children.forEach { recordInsets(it, insets, entries) }
}

@Composable
private fun rememberDimensionIsZLayer(stack: StackComponentStyle, state: PaywallState.Loaded.Components): Boolean =
    rememberUpdatedStackComponentState(stack, state).dimension is Dimension.ZLayer

@Suppress("CyclomaticComplexMethod")
@Composable
private fun firstVisibleLeaf(
    style: ComponentStyle,
    state: PaywallState.Loaded.Components,
): FirstVisibleLeaf? {
    return when (style) {
        is StackComponentStyle -> {
            val stack = rememberUpdatedStackComponentState(style, state)
            if (!stack.visible) return null
            var first: FirstVisibleLeaf? = null
            for (child in stack.children) {
                first = firstVisibleLeaf(child, state)
                if (first != null) {
                    first = first.copy(ancestors = first.ancestors + (style to child))
                    break
                }
            }
            first ?: FirstVisibleLeaf(style, false)
        }
        is ImageComponentStyle -> rememberUpdatedImageComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, it.size.width is SizeConstraint.Fill) else null
        }
        is VideoComponentStyle -> rememberUpdatedVideoComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, it.size.width is SizeConstraint.Fill) else null
        }
        is WebViewComponentStyle -> rememberUpdatedWebViewComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, style.size.width is SizeConstraint.Fill) else null
        }
        is TextComponentStyle -> rememberUpdatedTextComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, false) else null
        }
        is ButtonComponentStyle -> rememberUpdatedButtonVisibilityState(style, state).let {
            if (it.visible) firstVisibleWrapperLeaf(style, style.stackComponentStyle, state) else null
        }
        is CarouselComponentStyle -> rememberUpdatedCarouselComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, false) else null
        }
        is IconComponentStyle -> rememberUpdatedIconComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, false) else null
        }
        is PackageComponentStyle -> rememberUpdatedPackageComponentState(style, state).let {
            if (it.visible) firstVisibleWrapperLeaf(style, style.stackComponentStyle, state) else null
        }
        is CountdownComponentStyle -> firstVisibleWrapperLeaf(style, rememberActiveCountdownStack(style), state)
        is TabsComponentStyle -> rememberUpdatedTabsComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, false) else null
        }
        is TimelineComponentStyle -> rememberUpdatedTimelineComponentState(style, state).let {
            if (it.visible) FirstVisibleLeaf(style, false) else null
        }
        else -> if (style.visible) FirstVisibleLeaf(style, false) else null
    }
}

@Composable
private fun firstVisibleWrapperLeaf(
    style: ComponentStyle,
    stack: StackComponentStyle,
    state: PaywallState.Loaded.Components,
): FirstVisibleLeaf? =
    if (rememberUpdatedStackComponentState(stack, state).visible) FirstVisibleLeaf(style, false) else null
