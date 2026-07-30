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
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regression test for a real customer paywall (`test_k`): a `Carousel` with no explicit `size`
 * (schema default `Fit`/`Fit`) whose page and page content are both `Fill`/`Fill` rendered
 * completely blank on Android, because every V2 paywall's root content is wrapped in a default
 * `verticalScroll` (`LoadedPaywallComponents.kt`) unless the root stack itself already scrolls
 * vertically. That relaxes the ambient height constraint to unbounded by the time it reaches the
 * carousel's `HorizontalPager`, and a `Fill`-height page (and its `Fill`-height content) collapses
 * to exactly zero via `Modifier.weight`'s unbounded-constraint fallback — see
 * `StackComponentView.kt`'s `mainAxisUnbounded` tracking for the fix.
 */
@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class CarouselFillCollapseTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `carousel page and content both Fill Fill render non-zero under the default root scroll`() {
        // A real leaf with actual content, matching test_k's [TextComponent, WebViewComponent]
        // content: an empty decorative box has zero intrinsic size and would correctly measure to
        // 0 under any unbounded constraint (nothing to fix there) -- the bug this test guards
        // against is that the CONTAINING stacks (page, content) collapse the incoming constraint
        // itself via `weight`, hiding content that would otherwise render fine. `web_view`'s own
        // fallback for a genuinely-unbounded Fill axis is covered separately in
        // WebViewEffectiveSizeTest; this isolates the Stack-level fix.
        val leaf = TextComponent(
            text = LocalizationKey("dummyKey"),
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            size = Size(width = Fit(), height = Fit()),
        )
        val content = StackComponent(
            components = listOf(leaf),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.CENTER,
            ),
            size = Size(width = Fill, height = Fill),
        )
        val page = StackComponent(
            components = listOf(content),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.CENTER,
            ),
            size = Size(width = Fill, height = Fill),
        )
        // No explicit `size` — matches the schema default (Fit/Fit) and test_k's real config.
        val carousel = CarouselComponent(
            pages = listOf(page),
            pageAlignment = VerticalAlignment.CENTER,
        )

        val state = FakePaywallState(components = listOf(carousel))
        val rootStack = state.stack as StackComponentStyle
        val carouselStyle = rootStack.children.filterIsInstance<CarouselComponentStyle>().single()

        var measuredHeight = -1

        composeTestRule.setContent {
            // Mirrors LoadedPaywallComponents.kt's real modifier chain on the root ComponentView:
            // Modifier.fillMaxSize().conditional(shouldWrapMainContentInVerticalScroll) { verticalScroll(...) }
            // — the default for every V2 paywall whose root stack doesn't itself scroll vertically.
            Box(Modifier.fillMaxSize().height(800.dp)) {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CarouselComponentView(
                        style = carouselStyle,
                        state = state,
                        clickHandler = {},
                        modifier = Modifier.onGloballyPositioned { measuredHeight = it.size.height },
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Before the fix, this measured exactly 0 (matching the customer's blank screenshot).
        assertThat(measuredHeight).isGreaterThan(0)
    }
}
