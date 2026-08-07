package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * `pages` is a plain list off the wire with no non-empty validation, and a page control reads the
 * carousel's logical page before anything guards on the list. The modulo behind that read throws on
 * a zero page count.
 */
@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class CarouselEmptyPagesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a carousel with no pages and a page control renders instead of crashing`() {
        val carousel = CarouselComponent(
            pages = emptyList(),
            pageAlignment = VerticalAlignment.CENTER,
            pageControl = CarouselComponent.PageControl(
                position = CarouselComponent.PageControl.Position.BOTTOM,
                active = indicator(),
                default = indicator(),
            ),
        )
        val state = FakePaywallState(components = listOf(carousel))
        val rootStack = state.stack as StackComponentStyle
        val carouselStyle = rootStack.children.filterIsInstance<CarouselComponentStyle>().single()

        composeTestRule.setContent {
            Box(Modifier.fillMaxSize()) {
                CarouselComponentView(style = carouselStyle, state = state, clickHandler = {})
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun indicator() = CarouselComponent.PageControl.Indicator(
        width = 8u,
        height = 8u,
        color = ColorScheme(light = ColorInfo.Hex(0xFF000000.toInt())),
    )
}
