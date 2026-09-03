package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.CarouselComponent
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
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

/**
 * Regression tests: a Fit-height carousel (the schema default) sizes itself to its tallest page,
 * and a Fill-height page must stretch to match rather than wrapping its own (much smaller) content
 * -- see `CarouselComponentView`, which leaves the Pager unpinned (so it keeps tracking the tallest
 * page) and pins only the Fill pages to that measured height.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class CarouselFillMatchesFixedSiblingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Fill page stretches to match a Fixed sibling page's height`() {
        val leaf = TextComponent(
            text = LocalizationKey("dummyKey"),
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = Fit(), height = Fit()),
        )
        val fillPage = StackComponent(
            components = listOf(leaf),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.CENTER,
            ),
            size = Size(width = Fill(), height = Fill()),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
        )
        val fixedSiblingPage = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(), height = Fixed(300u)),
        )
        // No explicit carousel `size` -- matches the schema default (Fit/Fit).
        val carousel = CarouselComponent(
            pages = listOf(fillPage, fixedSiblingPage),
            pageAlignment = VerticalAlignment.CENTER,
        )

        assertFillPageStretchesTo(carousel, expectedHeight = 300.dp)
    }

    @Test
    fun `Fill page stretches to match a sibling whose height comes from a nested Fixed descendant`() {
        // Matches a real customer paywall: the tall sibling page itself declares Fit/Fit, but wraps
        // a Fixed(350) content stack -- there is no top-level Fixed page to read from the schema,
        // only a real, naturally-tall measured page.
        val fillPage = StackComponent(
            components = listOf(
                StackComponent(
                    components = emptyList(),
                    size = Size(width = Fill(), height = Fill()),
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                ),
            ),
            size = Size(width = Fill(), height = Fill()),
        )
        val nestedFixedSiblingPage = StackComponent(
            components = listOf(
                StackComponent(
                    components = emptyList(),
                    size = Size(width = Fixed(350u), height = Fixed(350u)),
                ),
            ),
            size = Size(width = Fit(), height = Fit()),
        )
        val carousel = CarouselComponent(
            pages = listOf(fillPage, nestedFixedSiblingPage),
            pageAlignment = VerticalAlignment.CENTER,
        )

        assertFillPageStretchesTo(carousel, expectedHeight = 350.dp)
    }

    /**
     * Renders [carousel] under the real default root-scroll chain and asserts it measures to
     * [expectedHeight] (the tallest page) and that the visible Fill page (blue) stretches all the
     * way to the bottom edge, rather than wrapping its own content and leaving the rest blank.
     */
    private fun assertFillPageStretchesTo(carousel: CarouselComponent, expectedHeight: Dp) {
        val state = FakePaywallState(components = listOf(carousel))
        val rootStack = state.stack as StackComponentStyle
        val carouselStyle = rootStack.children.filterIsInstance<CarouselComponentStyle>().single()

        var measuredHeightPx = -1

        composeTestRule.setContent {
            // Mirrors LoadedPaywallComponents.kt's real modifier chain on the root ComponentView.
            Box(Modifier.fillMaxSize().height(800.dp)) {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CarouselComponentView(
                        style = carouselStyle,
                        state = state,
                        clickHandler = {},
                        modifier = Modifier
                            .testTag("carousel")
                            .onGloballyPositioned { measuredHeightPx = it.size.height },
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val expectedHeightPx = with(composeTestRule.density) { expectedHeight.roundToPx() }
        // Before the fix, this wrapped to the Fill page's own (much smaller) content height.
        assertThat(measuredHeightPx).isEqualTo(expectedHeightPx)

        composeTestRule.onNodeWithTag("carousel")
            .assertPixelColorEquals(
                color = Color.Blue,
                startX = 10,
                startY = expectedHeightPx - 10,
                width = 1,
                height = 1,
            )
    }
}
