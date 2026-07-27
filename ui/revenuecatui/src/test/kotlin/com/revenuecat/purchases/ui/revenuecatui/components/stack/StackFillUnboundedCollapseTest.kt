package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

/**
 * Regression tests for the `Fill`-child-collapses-under-an-unbounded-main-axis bug fixed via
 * `mainAxisUnbounded` tracking in `StackComponentView.kt`. See `CarouselFillCollapseTest` for the
 * broader real-world repro (a Carousel with no scroll declared at all); these focus on the
 * `Stack`-level mechanism directly, including the earlier, narrower "explicit `overflow: scroll`"
 * case this fix subsumes.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class StackFillUnboundedCollapseTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val styleFactory = StyleFactory()

    @Test
    fun `Fit stack with explicit vertical scroll no longer collapses a Fill child`() {
        // A real leaf with actual content: an empty decorative box has zero intrinsic size and
        // would correctly measure to 0 under any unbounded constraint regardless of this fix
        // (nothing to rescue there). The bug is that the CONTAINING stack collapses the incoming
        // constraint itself via `weight`, hiding content that would otherwise render fine.
        val child = TextComponent(
            text = LocalizationKey("dummy"),
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = Fill, height = Fill),
        )
        val stack = StackComponent(
            components = listOf(child),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.START,
            ),
            size = Size(width = Fit(), height = Fit()),
            overflow = StackComponent.Overflow.SCROLL,
        )
        val style = styleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        var measuredHeight = -1
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().height(800.dp)) {
                StackComponentView(
                    style = style,
                    state = FakePaywallState(components = emptyList()),
                    clickHandler = {},
                    modifier = Modifier.onGloballyPositioned { measuredHeight = it.size.height },
                )
            }
        }

        composeTestRule.waitForIdle()
        // Before the fix, this measured exactly 0 (Modifier.weight's unbounded-constraint fallback).
        assertThat(measuredHeight).isGreaterThan(0)
    }

    @Test
    fun `Fixed stack under an ambient-unbounded scroll still distributes weight between Fill siblings`() {
        // Guards the probe's placement: it must observe the constraint AFTER this stack's own
        // .size() is applied, not the raw incoming constraint. A Fixed-sized stack establishes its
        // own bound regardless of what's above it, so its Fill children must keep real weight()
        // distribution -- if the probe were placed before .size(), an ambient scroll would wrongly
        // disable weight() even though Fixed(200) already gives the Row a real bound.
        assertTwoFillSiblingsSplitEvenly(stackSize = Size(width = Fixed(200u), height = Fixed(50u))) { content ->
            Box(Modifier.fillMaxSize().height(800.dp)) {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        }
    }

    @Test
    fun `bounded Fill siblings still split proportionally, no scroll involved`() {
        assertTwoFillSiblingsSplitEvenly(stackSize = Size(width = Fixed(200u), height = Fixed(50u))) { content ->
            Box(Modifier.fillMaxSize().height(800.dp)) {
                content()
            }
        }
    }

    /**
     * Renders a horizontal stack of [stackSize] with two `Fill`-width children (red, then blue)
     * wrapped in [wrapper], and asserts each occupies roughly its own half of the stack's width --
     * i.e. that `Modifier.weight` is still doing real proportional distribution, not that one
     * child silently took everything (which is what happens if `weight` gets disabled when it
     * shouldn't be).
     */
    private fun assertTwoFillSiblingsSplitEvenly(
        stackSize: Size,
        wrapper: @Composable (content: @Composable () -> Unit) -> Unit,
    ) {
        val redChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill, height = Fill),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        )
        val blueChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill, height = Fill),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
        )
        val stack = StackComponent(
            components = listOf(redChild, blueChild),
            dimension = Dimension.Horizontal(alignment = VerticalAlignment.CENTER, distribution = FlexDistribution.START),
            size = stackSize,
        )
        val style = styleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        composeTestRule.setContent {
            wrapper {
                StackComponentView(
                    style = style,
                    state = FakePaywallState(components = emptyList()),
                    clickHandler = {},
                    modifier = Modifier.testTag("stack"),
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stack")
            // Deep into the first quarter: must be red.
            .assertPixelColorEquals(color = Color.Red, startX = 10, startY = 10, width = 1, height = 1)
            // Deep into the last quarter: must be blue. Would still be red if weight() were
            // incorrectly disabled (the first Fill child would greedily take the whole width).
            .assertPixelColorEquals(color = Color.Blue, startX = 190, startY = 10, width = 1, height = 1)
    }
}
