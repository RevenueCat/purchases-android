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
 * `Stack`-level mechanism directly, on both axes, including the earlier, narrower "explicit
 * `overflow: scroll`" case this fix subsumes.
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
        assertScrollingFitStackKeepsFillChild(vertical = true)
    }

    @Test
    fun `Fit stack with explicit horizontal scroll no longer collapses a Fill child`() {
        // The horizontal counterpart of the fix path (StackComponentView's Horizontal branch): a
        // Fill-width child under a Fit-width stack whose own width axis scrolls (so it's unbounded).
        assertScrollingFitStackKeepsFillChild(vertical = false)
    }

    @Test
    fun `Fixed-height vertical stack under an ambient vertical scroll still distributes weight`() {
        // Guards the probe's placement: it must observe the constraint AFTER this stack's own
        // .size() is applied, not the raw incoming one. Here the ambient verticalScroll relaxes the
        // vertical (main) axis to unbounded, but the stack's own Fixed(200) height re-establishes a
        // bound, so its two Fill-height children must still split it 50/50 via weight(). If the
        // probe read the constraint BEFORE .size(), it would see the ambient unbounded axis and
        // wrongly disable weight() -- this test would then fail.
        assertTwoFillSiblingsSplitEvenly(horizontal = false) { content ->
            Box(Modifier.fillMaxSize().height(800.dp)) {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        }
    }

    @Test
    fun `Fill child still stretches under the real root scroll chain`() {
        // Regression (template_019): LoadedPaywallComponents applies `fillMaxSize().verticalScroll()`
        // to the root stack's own node, so the stack sees minHeight = viewport with maxHeight =
        // Infinity. Compose distributes that min, so weight still works and must NOT be skipped --
        // treating "max is Infinity" alone as unbounded made every Fill child on such a paywall wrap
        // its content instead of stretching. (Wrapping the scroll around a parent Box does not
        // reproduce this: Box relaxes its child's min to 0.)
        val fillChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(), height = Fill()),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        )
        val root = StackComponent(
            components = listOf(
                StackComponent(components = emptyList(), size = Size(width = Fill(), height = Fixed(100u))),
                fillChild,
            ),
            dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
            size = Size(width = Fill(), height = Fill()),
        )
        val style = styleFactory.create(root).getOrThrow().componentStyle as StackComponentStyle

        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().height(800.dp)) {
                StackComponentView(
                    style = style,
                    state = FakePaywallState(components = emptyList()),
                    clickHandler = {},
                    modifier = Modifier
                        .testTag("stack")
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            }
        }

        composeTestRule.waitForIdle()
        // The Fill child must stretch through the remaining height, so near the bottom edge is red.
        // Without weight it wraps its (empty) content to zero and nothing is drawn there.
        val node = composeTestRule.onNodeWithTag("stack")
        val bottom = node.fetchSemanticsNode().size.height - 5
        node.assertPixelColorEquals(Color.Red, startX = 10, startY = bottom, width = 1, height = 1)
    }

    @Test
    fun `bounded Fill siblings still split proportionally, no scroll involved`() {
        assertTwoFillSiblingsSplitEvenly(horizontal = true) { content ->
            Box(Modifier.fillMaxSize().height(800.dp)) {
                content()
            }
        }
    }

    @Test
    fun `Fill child with maximum does not occupy its entire weighted slot`() {
        val constrainedChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(max = 20u), height = Fill()),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        )
        val root = StackComponent(
            components = listOf(constrainedChild),
            dimension = Dimension.Horizontal(VerticalAlignment.TOP, FlexDistribution.START),
            size = Size(width = Fixed(100u), height = Fixed(20u)),
        )
        val style = styleFactory.create(root).getOrThrow().componentStyle as StackComponentStyle

        composeTestRule.setContent {
            StackComponentView(
                style = style,
                state = FakePaywallState(components = emptyList()),
                clickHandler = {},
                modifier = Modifier.testTag("stack"),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stack").assertPixelColorEquals(
            Color.Red,
            startX = 5,
            startY = 5,
            width = 1,
            height = 1,
        )
    }

    @Test
    fun `horizontal Fill minimum is allocated before its sibling`() {
        assertConstrainedFillSiblings(
            horizontal = true,
            firstConstraint = Fill(min = 80u),
            firstColorPosition = 75,
            secondColorPosition = 85,
        )
    }

    @Test
    fun `vertical Fill maximum releases space to its sibling`() {
        assertConstrainedFillSiblings(
            horizontal = false,
            firstConstraint = Fill(max = 20u),
            firstColorPosition = 15,
            secondColorPosition = 85,
        )
    }

    private fun assertScrollingFitStackKeepsFillChild(vertical: Boolean) {
        // A real leaf with actual content: an empty decorative box has zero intrinsic size and
        // would correctly measure to 0 under any unbounded constraint regardless of this fix
        // (nothing to rescue there). The bug is that the CONTAINING stack collapses the incoming
        // constraint itself via `weight`, hiding content that would otherwise render fine.
        val child = TextComponent(
            text = LocalizationKey("dummy"),
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = Fill(), height = Fill()),
        )
        val dimension = if (vertical) {
            Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START)
        } else {
            Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START)
        }
        val stack = StackComponent(
            components = listOf(child),
            dimension = dimension,
            size = Size(width = Fit(), height = Fit()),
            overflow = StackComponent.Overflow.SCROLL,
        )
        val style = styleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        var measuredSize = -1
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().height(800.dp)) {
                StackComponentView(
                    style = style,
                    state = FakePaywallState(components = emptyList()),
                    clickHandler = {},
                    modifier = Modifier.onGloballyPositioned {
                        measuredSize = if (vertical) it.size.height else it.size.width
                    },
                )
            }
        }

        composeTestRule.waitForIdle()
        // Before the fix, this measured exactly 0 (Modifier.weight's unbounded-constraint fallback).
        assertThat(measuredSize).isGreaterThan(0)
    }

    /**
     * Renders a [horizontal] (else vertical) stack of `mainAxis` x `crossAxis` with two `Fill`
     * children (red, then blue) wrapped in [wrapper], and asserts each occupies its own half of the
     * main axis -- i.e. that `Modifier.weight` is still doing real proportional distribution, not
     * that one child silently took everything (which is what happens if `weight` gets disabled when
     * it shouldn't). Pixel positions are derived via the current density so the assertions hold
     * regardless of the screen density Robolectric runs at.
     */
    private fun assertTwoFillSiblingsSplitEvenly(
        horizontal: Boolean,
        wrapper: @Composable (content: @Composable () -> Unit) -> Unit,
    ) {
        val mainAxis = 200.dp
        val crossAxis = 100.dp
        val redChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(), height = Fill()),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        )
        val blueChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(), height = Fill()),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
        )
        val stack = StackComponent(
            components = listOf(redChild, blueChild),
            dimension = if (horizontal) {
                Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START)
            } else {
                Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START)
            },
            size = if (horizontal) {
                Size(width = Fixed(mainAxis.value.toUInt()), height = Fixed(crossAxis.value.toUInt()))
            } else {
                Size(width = Fixed(crossAxis.value.toUInt()), height = Fixed(mainAxis.value.toUInt()))
            },
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
        val (mainPx, crossPx) = with(composeTestRule.density) { mainAxis.roundToPx() to crossAxis.roundToPx() }
        // Deep in the first main-axis quarter must be red, deep in the last quarter blue. If weight()
        // were wrongly disabled the first Fill child would greedily take everything, so the last
        // quarter would still be red.
        val firstQuarter = mainPx / 4
        val lastQuarter = mainPx * 3 / 4
        val crossMid = crossPx / 2
        val (redX, redY) = if (horizontal) firstQuarter to crossMid else crossMid to firstQuarter
        val (blueX, blueY) = if (horizontal) lastQuarter to crossMid else crossMid to lastQuarter
        composeTestRule.onNodeWithTag("stack")
            .assertPixelColorEquals(color = Color.Red, startX = redX, startY = redY, width = 1, height = 1)
            .assertPixelColorEquals(color = Color.Blue, startX = blueX, startY = blueY, width = 1, height = 1)
    }

    private fun assertConstrainedFillSiblings(
        horizontal: Boolean,
        firstConstraint: Fill,
        firstColorPosition: Int,
        secondColorPosition: Int,
    ) {
        val firstChild = StackComponent(
            components = emptyList(),
            size = if (horizontal) {
                Size(width = firstConstraint, height = Fill())
            } else {
                Size(width = Fill(), height = firstConstraint)
            },
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        )
        val secondChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(), height = Fill()),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
        )
        val stack = StackComponent(
            components = listOf(firstChild, secondChild),
            dimension = if (horizontal) {
                Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START)
            } else {
                Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START)
            },
            size = if (horizontal) {
                Size(width = Fixed(100u), height = Fixed(20u))
            } else {
                Size(width = Fixed(20u), height = Fixed(100u))
            },
        )
        val style = styleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        composeTestRule.setContent {
            StackComponentView(
                style = style,
                state = FakePaywallState(components = emptyList()),
                clickHandler = {},
                modifier = Modifier.testTag("stack"),
            )
        }

        composeTestRule.waitForIdle()
        val firstPositionPx = with(composeTestRule.density) { firstColorPosition.dp.roundToPx() }
        val secondPositionPx = with(composeTestRule.density) { secondColorPosition.dp.roundToPx() }
        val crossPositionPx = with(composeTestRule.density) { 10.dp.roundToPx() }
        val (firstX, firstY) = if (horizontal) {
            firstPositionPx to crossPositionPx
        } else {
            crossPositionPx to firstPositionPx
        }
        val (secondX, secondY) = if (horizontal) {
            secondPositionPx to crossPositionPx
        } else {
            crossPositionPx to secondPositionPx
        }
        composeTestRule.onNodeWithTag("stack")
            .assertPixelColorEquals(Color.Red, firstX, firstY, width = 1, height = 1)
            .assertPixelColorEquals(Color.Blue, secondX, secondY, width = 1, height = 1)
    }
}
