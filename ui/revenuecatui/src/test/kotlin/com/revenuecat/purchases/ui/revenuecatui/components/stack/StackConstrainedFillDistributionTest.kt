package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
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

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class StackConstrainedFillDistributionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val styleFactory = StyleFactory()

    @Test
    fun `horizontal capped Fill children preserve space between distribution`() {
        assertCappedFillDistribution(
            horizontal = true,
            distribution = FlexDistribution.SPACE_BETWEEN,
            firstColorPosition = 10,
            secondColorPosition = 90,
        )
    }

    @Test
    fun `horizontal capped Fill children preserve space around distribution`() {
        assertCappedFillDistribution(
            horizontal = true,
            distribution = FlexDistribution.SPACE_AROUND,
            firstColorPosition = 25,
            secondColorPosition = 75,
        )
    }

    @Test
    fun `horizontal capped Fill children preserve space evenly distribution`() {
        assertCappedFillDistribution(
            horizontal = true,
            distribution = FlexDistribution.SPACE_EVENLY,
            firstColorPosition = 30,
            secondColorPosition = 70,
        )
    }

    @Test
    fun `vertical capped Fill children preserve space between distribution`() {
        assertCappedFillDistribution(
            horizontal = false,
            distribution = FlexDistribution.SPACE_BETWEEN,
            firstColorPosition = 10,
            secondColorPosition = 90,
        )
    }

    @Test
    fun `vertical capped Fill children preserve space around distribution`() {
        assertCappedFillDistribution(
            horizontal = false,
            distribution = FlexDistribution.SPACE_AROUND,
            firstColorPosition = 25,
            secondColorPosition = 75,
        )
    }

    @Test
    fun `vertical capped Fill children preserve space evenly distribution`() {
        assertCappedFillDistribution(
            horizontal = false,
            distribution = FlexDistribution.SPACE_EVENLY,
            firstColorPosition = 30,
            secondColorPosition = 70,
        )
    }

    private fun assertCappedFillDistribution(
        horizontal: Boolean,
        distribution: FlexDistribution,
        firstColorPosition: Int,
        secondColorPosition: Int,
    ) {
        val childSize = if (horizontal) {
            Size(width = Fill(max = 20u), height = Fill())
        } else {
            Size(width = Fill(), height = Fill(max = 20u))
        }
        val firstChild = StackComponent(
            components = emptyList(),
            size = childSize,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
        )
        val secondChild = StackComponent(
            components = emptyList(),
            size = childSize,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
        )
        val stack = StackComponent(
            components = listOf(firstChild, secondChild),
            dimension = if (horizontal) {
                Dimension.Horizontal(VerticalAlignment.CENTER, distribution)
            } else {
                Dimension.Vertical(HorizontalAlignment.CENTER, distribution)
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
