package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.StackComponent.Overflow
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

/**
 * Fill children with a min/max share a 100dp main axis (cross axis 20dp). Each test renders the stack and samples
 * the color at a few main-axis positions (in dp) to verify where every child ended up.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class StackFillMinMaxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val styleFactory = StyleFactory()

    @Test
    fun `maximum releases space to Fill sibling`() = assertBothOrientations(
        stack = { h -> stack(h, child(Color.Red, fill(h, max = 20u)), child(Color.Blue, fill(h))) },
        10 to Color.Red,
        30 to Color.Blue,
        90 to Color.Blue,
    )

    @Test
    fun `minimum is reserved before sharing space with Fill sibling`() = assertBothOrientations(
        stack = { h -> stack(h, child(Color.Red, fill(h, min = 80u)), child(Color.Blue, fill(h))) },
        10 to Color.Red,
        75 to Color.Red,
        90 to Color.Blue,
    )

    @Test
    fun `capped Fill children preserve space between distribution`() = assertBothOrientations(
        stack = { h -> twoCappedChildren(h, FlexDistribution.SPACE_BETWEEN) },
        10 to Color.Red,
        50 to Color.Green,
        90 to Color.Blue,
    )

    @Test
    fun `capped Fill children preserve space around distribution`() = assertBothOrientations(
        stack = { h -> twoCappedChildren(h, FlexDistribution.SPACE_AROUND) },
        5 to Color.Green,
        25 to Color.Red,
        75 to Color.Blue,
    )

    @Test
    fun `capped Fill children preserve space evenly distribution`() = assertBothOrientations(
        stack = { h -> twoCappedChildren(h, FlexDistribution.SPACE_EVENLY) },
        10 to Color.Green,
        30 to Color.Red,
        70 to Color.Blue,
    )

    @Test
    fun `non-Fill siblings and spacing are subtracted before Fill space is shared`() = assertBothOrientations(
        stack = { h ->
            stack(
                h,
                child(Color.Red, fixed(h, 30u)),
                child(Color.Blue, fill(h, max = 20u)),
                child(Color.Yellow, fill(h)),
                spacing = 10f,
            )
        },
        // Red 0..30, gap, Blue 40..60, gap, Yellow 70..100.
        15 to Color.Red,
        35 to Color.Green,
        50 to Color.Blue,
        65 to Color.Green,
        85 to Color.Yellow,
    )

    @Test
    fun `min and max include the child's resolved margin`() = assertBothOrientations(
        stack = { h ->
            val margin = if (h) Padding(leading = 10.0, trailing = 10.0) else Padding(top = 10.0, bottom = 10.0)
            stack(
                h,
                child(Color.Red, fill(h, min = 50u), override(PartialStackComponent(margin = margin))),
                child(Color.Blue, fill(h)),
            )
        },
        // Red occupies 10..60 inside a 0..70 slot; Blue gets the remaining 70..100.
        5 to Color.Green,
        30 to Color.Red,
        65 to Color.Green,
        75 to Color.Blue,
    )

    @Test
    fun `maximum applied through an override is honored`() = assertBothOrientations(
        stack = { h ->
            stack(
                h,
                child(Color.Red, fill(h), override(PartialStackComponent(size = fill(h, max = 20u)))),
                child(Color.Blue, fill(h)),
            )
        },
        10 to Color.Red,
        30 to Color.Blue,
    )

    @Test
    fun `Fill child overridden to Fixed is not given Fill space`() = assertBothOrientations(
        stack = { h ->
            stack(
                h,
                child(Color.Red, fill(h, max = 50u), override(PartialStackComponent(size = fixed(h, 20u)))),
                child(Color.Blue, fill(h)),
            )
        },
        10 to Color.Red,
        30 to Color.Blue,
    )

    @Test
    fun `text size override replaces its base constrained Fill allocation`() {
        val text = TextComponent(
            text = LocalizationKey("dummy"),
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            size = Size(width = Fill(max = 60u), height = Fill()),
            overrides = listOf(
                ComponentOverride(
                    conditions = emptyList(),
                    properties = PartialTextComponent(
                        size = Size(width = Fixed(180u), height = Fill()),
                    ),
                ),
            ),
        )
        val stack = StackComponent(
            components = listOf(
                text,
                child(Color.Blue, Size(width = Fill(), height = Fill())),
            ),
            dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
            size = Size(width = Fixed(300u), height = Fixed(20u)),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())),
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

        fun px(dp: Int) = with(composeTestRule.density) { dp.dp.roundToPx() }
        composeTestRule.onNodeWithTag("stack")
            .assertPixelColorEquals(Color.Red, px(120), px(10), width = 1, height = 1)
            .assertPixelColorEquals(Color.Blue, px(240), px(10), width = 1, height = 1)
    }

    @Test
    fun `hidden child does not shift its siblings' constraints`() = assertBothOrientations(
        stack = { h ->
            stack(
                h,
                child(Color.Yellow, fill(h, max = 20u), override(PartialStackComponent(visible = false))),
                child(Color.Red, fill(h, max = 20u)),
                child(Color.Blue, fill(h)),
            )
        },
        // If constraints were matched by index, Red would be treated as unlimited and Blue capped at 20.
        10 to Color.Red,
        30 to Color.Blue,
        90 to Color.Blue,
    )

    @Test
    fun `hidden limited Fill child does not expand a Fit stack`() {
        val styles = listOf(true, false).associateWith { horizontal ->
            styleFactory.create(
                stack(
                    horizontal,
                    child(Color.Yellow, fill(horizontal, max = 20u), override(PartialStackComponent(visible = false))),
                    child(Color.Red, fixed(horizontal, 20u)),
                    size = sizeWithMainAxis(horizontal, Fit()),
                ),
            ).getOrThrow().componentStyle as StackComponentStyle
        }
        composeTestRule.setContent {
            Column {
                styles.forEach { (horizontal, style) ->
                    Box(
                        modifier = Modifier.requiredSize(
                            width = if (horizontal) 100.dp else 20.dp,
                            height = if (horizontal) 20.dp else 100.dp,
                        ),
                    ) {
                        StackComponentView(
                            style = style,
                            state = FakePaywallState(components = emptyList()),
                            clickHandler = {},
                            modifier = Modifier.testTag(tag(horizontal)),
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        styles.keys.forEach { horizontal ->
            with(composeTestRule.onNodeWithTag(tag(horizontal))) {
                if (horizontal) {
                    assertWidthIsEqualTo(20.dp)
                } else {
                    assertHeightIsEqualTo(20.dp)
                }
            }
        }
    }

    @Test
    fun `scrolling stack shares its own fixed size when the scroll axis is unbounded`() = assertBothOrientations(
        stack = { h ->
            stack(
                h,
                child(Color.Red, fill(h, min = 30u)),
                child(Color.Blue, fill(h, max = 20u)),
                overflow = Overflow.SCROLL,
            )
        },
        // The scroll lifts the max constraint but keeps the stack's 100dp minimum, which is what gets shared:
        // Blue is capped at 20 and Red takes the remaining 80.
        10 to Color.Red,
        75 to Color.Red,
        90 to Color.Blue,
    )

    @Test
    fun `cross-axis min and max are applied by the child itself`() {
        // No main-axis min/max, so this is a plain Row: the child's own size modifier must clamp the cross axis.
        val stack = stack(
            horizontal = true,
            child(Color.Red, Size(width = Fill(), height = Fill(max = 10u))),
            child(Color.Blue, Size(width = Fill(), height = Fill(min = 30u))),
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

        fun px(dp: Int) = with(composeTestRule.density) { dp.dp.roundToPx() }
        composeTestRule.onNodeWithTag("stack")
            // Red is 10dp tall, centered in the 20dp stack: 5..15.
            .assertPixelColorEquals(Color.Green, px(25), px(2), width = 1, height = 1)
            .assertPixelColorEquals(Color.Red, px(25), px(10), width = 1, height = 1)
            // Blue's minimum exceeds the stack, so it fills all 20dp.
            .assertPixelColorEquals(Color.Blue, px(75), px(2), width = 1, height = 1)
            .assertPixelColorEquals(Color.Blue, px(75), px(18), width = 1, height = 1)
    }

    private fun twoCappedChildren(horizontal: Boolean, distribution: FlexDistribution) = stack(
        horizontal,
        child(Color.Red, fill(horizontal, max = 20u)),
        child(Color.Blue, fill(horizontal, max = 20u)),
        distribution = distribution,
    )

    private fun fill(horizontal: Boolean, min: UInt? = null, max: UInt? = null): Size =
        sizeWithMainAxis(horizontal, Fill(min = min, max = max))

    private fun fixed(horizontal: Boolean, value: UInt): Size = sizeWithMainAxis(horizontal, Fixed(value))

    private fun sizeWithMainAxis(horizontal: Boolean, mainAxis: SizeConstraint): Size =
        if (horizontal) Size(width = mainAxis, height = Fill()) else Size(width = Fill(), height = mainAxis)

    private fun override(properties: PartialStackComponent) =
        ComponentOverride(conditions = emptyList(), properties = properties)

    private fun child(
        color: Color,
        size: Size,
        vararg overrides: ComponentOverride<PartialStackComponent>,
    ) = StackComponent(
        components = emptyList(),
        size = size,
        backgroundColor = ColorScheme(light = ColorInfo.Hex(color.toArgb())),
        overrides = overrides.toList(),
    )

    private fun stack(
        horizontal: Boolean,
        vararg children: StackComponent,
        distribution: FlexDistribution = FlexDistribution.START,
        spacing: Float? = null,
        overflow: Overflow? = null,
        size: Size? = null,
    ) = StackComponent(
        components = children.toList(),
        dimension = if (horizontal) {
            Dimension.Horizontal(VerticalAlignment.CENTER, distribution)
        } else {
            Dimension.Vertical(HorizontalAlignment.CENTER, distribution)
        },
        size = size ?: if (horizontal) {
            Size(width = Fixed(100u), height = Fixed(20u))
        } else {
            Size(width = Fixed(20u), height = Fixed(100u))
        },
        spacing = spacing,
        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())),
        overflow = overflow,
    )

    /** Renders the horizontal and vertical variants of [stack] side by side and samples both. */
    private fun assertBothOrientations(
        stack: (horizontal: Boolean) -> StackComponent,
        vararg expected: Pair<Int, Color>,
    ) {
        val styles = listOf(true, false).associateWith { horizontal ->
            styleFactory.create(stack(horizontal)).getOrThrow().componentStyle as StackComponentStyle
        }
        composeTestRule.setContent {
            Column {
                styles.forEach { (horizontal, style) ->
                    StackComponentView(
                        style = style,
                        state = FakePaywallState(components = emptyList()),
                        clickHandler = {},
                        modifier = Modifier.testTag(tag(horizontal)),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val crossAxisPx = with(composeTestRule.density) { 10.dp.roundToPx() }
        styles.keys.forEach { horizontal ->
            val node = composeTestRule.onNodeWithTag(tag(horizontal))
            expected.forEach { (positionDp, color) ->
                val mainAxisPx = with(composeTestRule.density) { positionDp.dp.roundToPx() }
                val (x, y) = if (horizontal) mainAxisPx to crossAxisPx else crossAxisPx to mainAxisPx
                node.assertPixelColorEquals(color, x, y, width = 1, height = 1)
            }
        }
    }

    private fun tag(horizontal: Boolean) = if (horizontal) "horizontal" else "vertical"
}
