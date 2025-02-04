@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Horizontal
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStates
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf

private const val DURATION_MS_CROSS_FADE = 220

@Composable
internal fun TabsComponentView(
    style: TabsComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Get a StackComponentState that calculates the overridden properties we should use.
    val tabsState = rememberUpdatedTabsComponentState(
        style = style,
        paywallState = state,
    )
    if (!tabsState.visible) return

    val backgroundColorStyle = tabsState.backgroundColor?.forCurrentTheme
    val borderStyle = tabsState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = tabsState.shadow?.let { rememberShadowStyle(shadow = it) }

    AnimatedContent(
        targetState = state.selectedTabIndex,
        modifier = modifier
            .padding(tabsState.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, tabsState.shape) }
            .applyIfNotNull(backgroundColorStyle) { background(it, tabsState.shape) }
            .clip(tabsState.shape)
            .applyIfNotNull(borderStyle) { border(it, tabsState.shape).padding(it.width) }
            .padding(tabsState.padding),
        transitionSpec = {
            fadeIn(animationSpec = tween(DURATION_MS_CROSS_FADE, delayMillis = 0))
                .togetherWith(fadeOut(animationSpec = tween(DURATION_MS_CROSS_FADE)))
        },
    ) { selectedTabIndex ->
        // Coerce it, just in case we get an out-of-range value.
        val tab = tabsState.tabs[selectedTabIndex.coerceIn(0..tabsState.tabs.lastIndex)]
        StackComponentView(
            style = tab.stack,
            state = state,
            clickHandler = clickHandler,
        )
    }
}

private class SelectedTabIndexProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = sequenceOf(0, 1, 2)
}

@Suppress("LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TabsComponentView_Preview(
    @PreviewParameter(SelectedTabIndexProvider::class) selectedTabIndex: Int,
) {
    val boldWhenSelectedOverride = PresentedOverrides(
        introOffer = null,
        multipleIntroOffers = null,
        states = PresentedStates(
            selected = LocalizedTextPartial(
                from = PartialTextComponent(fontWeight = FontWeight.EXTRA_BOLD),
                using = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("dummy") to LocalizationData.Text("dummy"),
                    ),
                ),
                aliases = emptyMap(),
                fontAliases = emptyMap(),
            ).getOrThrow(),
        ),
        conditions = null,
    )

    val controlButtons = previewStackComponentStyle(
        children = listOf(
            TabControlButtonComponentStyle(
                tabIndex = 0,
                stack = previewStackComponentStyle(
                    children = listOf(
                        previewTextComponentStyle(
                            text = "Tab 1",
                            size = Size(width = Fit, height = Fit),
                            tabIndex = 0,
                            overrides = boldWhenSelectedOverride,
                        ),
                    ),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            TabControlButtonComponentStyle(
                tabIndex = 1,
                stack = previewStackComponentStyle(
                    children = listOf(
                        previewTextComponentStyle(
                            text = "Tab 2",
                            size = Size(width = Fit, height = Fit),
                            tabIndex = 1,
                            overrides = boldWhenSelectedOverride,
                        ),
                    ),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            TabControlButtonComponentStyle(
                tabIndex = 2,
                stack = previewStackComponentStyle(
                    children = listOf(
                        previewTextComponentStyle(
                            text = "Tab 3",
                            size = Size(width = Fit, height = Fit),
                            tabIndex = 2,
                            overrides = boldWhenSelectedOverride,
                        ),
                    ),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
        ),
        dimension = Horizontal(alignment = VerticalAlignment.CENTER, FlexDistribution.CENTER),
        size = Size(width = Fit, height = Fit),
        spacing = 8.dp,
    )

    TabsComponentView(
        style = TabsComponentStyle(
            size = Size(width = Fill, height = Fill),
            padding = PaddingValues(all = 16.dp),
            margin = PaddingValues(all = 16.dp),
            backgroundColor = ColorStyles(
                light = ColorStyle.Solid(Color.LightGray),
                dark = ColorStyle.Solid(Color.DarkGray),
            ),
            shape = Shape.Rectangle(CornerRadiuses.Dp(all = 16.0)),
            border = BorderStyles(
                width = 8.dp,
                colors = ColorStyles(
                    light = ColorStyle.Solid(Color.Red),
                    dark = ColorStyle.Solid(Color.Blue),
                ),
            ),
            shadow = ShadowStyles(
                colors = ColorStyles(
                    light = ColorStyle.Solid(Color.Blue),
                    dark = ColorStyle.Solid(Color.Red),
                ),
                radius = 6.dp,
                x = 0.dp,
                y = 10.dp,
            ),
            tabs = nonEmptyListOf(
                TabsComponentStyle.Tab(
                    stack = previewStackComponentStyle(
                        children = listOf(
                            controlButtons,
                            previewTextComponentStyle(text = "Tab 1 content"),
                        ),
                        backgroundColor = ColorStyles(
                            light = ColorStyle.Solid(Color.Red),
                            dark = ColorStyle.Solid(Color.Blue),
                        ),
                    ),
                ),
                TabsComponentStyle.Tab(
                    stack = previewStackComponentStyle(
                        children = listOf(
                            controlButtons,
                            previewTextComponentStyle(text = "Tab 2 content"),
                        ),
                        backgroundColor = ColorStyles(
                            light = ColorStyle.Solid(Color.Yellow),
                            dark = ColorStyle.Solid(Color.Green),
                        ),
                    ),
                ),
                TabsComponentStyle.Tab(
                    stack = previewStackComponentStyle(
                        children = listOf(
                            controlButtons,
                            previewTextComponentStyle(text = "Tab 3 content"),
                        ),
                        backgroundColor = ColorStyles(
                            light = ColorStyle.Solid(Color.Blue),
                            dark = ColorStyle.Solid(Color.Red),
                        ),
                    ),
                ),
            ),
            overrides = null,
        ),
        state = previewEmptyState(initialSelectedTabIndex = selectedTabIndex),
        clickHandler = { },
    )
}

@Suppress("LongParameterList")
private fun previewStackComponentStyle(
    children: List<ComponentStyle>,
    dimension: Dimension = Vertical(alignment = HorizontalAlignment.CENTER, distribution = FlexDistribution.CENTER),
    size: Size = Size(width = Fill, height = Fill),
    spacing: Dp = 0.dp,
    backgroundColor: ColorStyles = ColorStyles(light = ColorStyle.Solid(Color.Transparent)),
    padding: PaddingValues = PaddingValues(all = 0.dp),
    margin: PaddingValues = PaddingValues(all = 0.dp),
    shape: Shape = Shape.Rectangle(CornerRadiuses.Dp(all = 0.0)),
    border: BorderStyles? = null,
    shadow: ShadowStyles? = null,
    tabIndex: Int? = null,
): StackComponentStyle =
    StackComponentStyle(
        children = children,
        dimension = dimension,
        size = size,
        spacing = spacing,
        backgroundColor = backgroundColor,
        padding = padding,
        margin = margin,
        shape = shape,
        border = border,
        shadow = shadow,
        badge = null,
        rcPackage = null,
        tabIndex = tabIndex,
        overrides = null,
    )
