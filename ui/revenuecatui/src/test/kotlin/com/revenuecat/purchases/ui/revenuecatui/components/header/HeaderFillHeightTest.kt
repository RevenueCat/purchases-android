package com.revenuecat.purchases.ui.revenuecatui.components.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.headerContentHeight
import com.revenuecat.purchases.ui.revenuecatui.components.style.HeaderComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * PWENG-383: the header is measured at its content height, matching iOS (`fixedSize(vertical: true)`) and web.
 * A Fill height in the header used to take the whole screen and push the main content out of view.
 */
@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class HeaderFillHeightTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Header with Fill height takes its content height`() {
        assertHeaderHeightDp(headerStack(height = Fill(), child = fixedChild()), expectedDp = CHILD_HEIGHT_DP)
    }

    @Test
    fun `Header with a Fill height child takes its content height`() {
        val fillChild = StackComponent(
            components = listOf(fixedChild()),
            size = Size(width = Fill(), height = Fill()),
        )

        assertHeaderHeightDp(headerStack(height = Fit(), child = fillChild), expectedDp = CHILD_HEIGHT_DP)
    }

    @Test
    fun `Header with Fill height keeps its minimum`() {
        assertHeaderHeightDp(headerStack(height = Fill(min = 120u), child = fixedChild()), expectedDp = 120)
    }

    @Test
    fun `Header with Fill height keeps its maximum`() {
        assertHeaderHeightDp(headerStack(height = Fill(max = 60u), child = fixedChild()), expectedDp = 60)
    }

    @Test
    fun `Header with Fixed height is unchanged`() {
        assertHeaderHeightDp(headerStack(height = Fixed(100u), child = fixedChild()), expectedDp = 100)
    }

    @Test
    fun `Header whose root scrolls vertically keeps the bounded height`() {
        val scrollingHeader = headerStack(
            height = Fill(),
            child = fixedChild(),
            overflow = StackComponent.Overflow.SCROLL,
        )

        assertHeaderHeightDp(scrollingHeader, expectedDp = CONTAINER_HEIGHT_DP)
    }

    @Test
    fun `Header with a nested vertical scroll keeps the bounded height`() {
        val scrollingChild = StackComponent(
            components = listOf(fixedChild()),
            overflow = StackComponent.Overflow.SCROLL,
        )

        assertHeaderHeightDp(headerStack(height = Fill(), child = scrollingChild), expectedDp = CONTAINER_HEIGHT_DP)
    }

    @Test
    fun `Header taller than the container is capped at the container height`() {
        val tallChild = StackComponent(
            components = emptyList(),
            size = Size(width = Fill(), height = Fixed(500u)),
        )

        assertHeaderHeightDp(headerStack(height = Fill(), child = tallChild), expectedDp = CONTAINER_HEIGHT_DP)
    }

    @Test
    fun `Header outside the overlay layout takes its content height`() {
        // Workflow headers fading out are drawn in a plain Box instead of the overlay layout.
        val style = StyleFactory()
            .create(HeaderComponent(stack = headerStack(height = Fill(), child = fixedChild())))
            .getOrThrow()
            .componentStyle as HeaderComponentStyle

        composeTestRule.setContent {
            Box(Modifier.size(CONTAINER_WIDTH_DP.dp, CONTAINER_HEIGHT_DP.dp)) {
                HeaderComponentView(
                    style = style,
                    state = FakePaywallState(components = emptyList()),
                    clickHandler = { },
                    modifier = Modifier
                        .testTag(TAG)
                        .fillMaxWidth()
                        .headerContentHeight(style),
                )
            }
        }
        composeTestRule.waitForIdle()

        val heightPx = composeTestRule.onNodeWithTag(TAG).fetchSemanticsNode().size.height
        assertThat(heightPx).isEqualTo(with(composeTestRule.density) { CHILD_HEIGHT_DP.dp.roundToPx() })
    }

    private fun headerStack(
        height: SizeConstraint,
        child: StackComponent,
        overflow: StackComponent.Overflow? = null,
    ) = StackComponent(
        components = listOf(child),
        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.SPACE_BETWEEN),
        size = Size(width = Fill(), height = height),
        overflow = overflow,
    )

    private fun fixedChild() = StackComponent(
        components = emptyList(),
        size = Size(width = Fill(), height = Fixed(CHILD_HEIGHT_DP.toUInt())),
    )

    private fun assertHeaderHeightDp(headerStack: StackComponent, expectedDp: Int) {
        val state = FakePaywallState(header = HeaderComponent(stack = headerStack))

        composeTestRule.setContent {
            LoadedPaywallComponents(
                state = state,
                clickHandler = { },
                modifier = Modifier.size(CONTAINER_WIDTH_DP.dp, CONTAINER_HEIGHT_DP.dp),
            )
        }
        composeTestRule.waitForIdle()

        val expectedPx = with(composeTestRule.density) { expectedDp.dp.roundToPx() }
        assertThat(state.headerHeightPx).isEqualTo(expectedPx)
    }

    private companion object {
        const val TAG = "header"
        const val CHILD_HEIGHT_DP = 80
        const val CONTAINER_WIDTH_DP = 300
        // Below Robolectric's default screen height, so the container is not clamped by the window.
        const val CONTAINER_HEIGHT_DP = 300
    }
}
