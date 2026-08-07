package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Regression test for SDK-4435. A `loop: true` carousel ran its pager over an unbounded index
 * space, so every auto-advance destroyed and rebuilt a page. Observed on device as a `web_view`
 * page fully reloading every ~8s for as long as the paywall was open.
 */
@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class CarouselLoopCompositionStabilityTest {

    private companion object {
        const val PAGE_ONE_TEXT = "pageOneText"
        const val PAGE_TWO_TEXT = "pageTwoText"
        const val MS_PER_PAGE = 3500
        const val MS_TRANSITION = 500
        // Two pages padded with two clones per side, so each page lands on three ring indices.
        // This is the memory ceiling the ticket exists to bound: if the clone pad grows, weigh the
        // extra live pages rather than just updating the number.
        const val EXPECTED_INSTANCES_PER_PAGE = 3
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a looping carousel composes each page a bounded number of times`() {
        setLoopingCarouselContent()

        composeTestRule.onAllNodesWithText(PAGE_ONE_TEXT, useUnmergedTree = true)
            .assertCountEquals(EXPECTED_INSTANCES_PER_PAGE)
        composeTestRule.onAllNodesWithText(PAGE_TWO_TEXT, useUnmergedTree = true)
            .assertCountEquals(EXPECTED_INSTANCES_PER_PAGE)
    }

    @Test
    fun `a looping carousel never rebuilds its pages while auto-advancing`() {
        setLoopingCarouselContent()

        val pageOneBefore = nodeIdsFor(PAGE_ONE_TEXT)
        val pageTwoBefore = nodeIdsFor(PAGE_TWO_TEXT)

        // Three full cycles: the old unbounded pager would have churned six indices by now.
        repeat(3 * 2) { advanceOnePage() }

        // Semantics ids are per LayoutNode, so a destroyed-and-recomposed page shows up as a new id.
        assertThat(nodeIdsFor(PAGE_ONE_TEXT)).isEqualTo(pageOneBefore)
        assertThat(nodeIdsFor(PAGE_TWO_TEXT)).isEqualTo(pageTwoBefore)
    }

    @Test
    fun `a looping carousel keeps advancing forward past its last page`() {
        setLoopingCarouselContent()

        // Without the re-centre the pager slides into the trailing clones and sticks there.
        val centeredPages = mutableListOf(centeredPageText())
        repeat(4) {
            advanceOnePage()
            centeredPages += centeredPageText()
        }

        assertThat(centeredPages).containsExactly(
            PAGE_ONE_TEXT,
            PAGE_TWO_TEXT,
            PAGE_ONE_TEXT,
            PAGE_TWO_TEXT,
            PAGE_ONE_TEXT,
        )
    }

    private fun advanceOnePage() {
        composeTestRule.mainClock.advanceTimeBy((MS_PER_PAGE + MS_TRANSITION).toLong())
        composeTestRule.waitForIdle()
    }

    private fun nodeIdsFor(text: String): Set<Int> = nodesFor(text).map { it.id }.toSet()

    private fun centeredPageText(): String {
        val rootCenterX = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot.center.x
        return listOf(PAGE_ONE_TEXT, PAGE_TWO_TEXT)
            .minBy { text -> nodesFor(text).minOf { abs(it.boundsInRoot.center.x - rootCenterX) } }
    }

    private fun nodesFor(text: String): List<SemanticsNode> =
        composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun setLoopingCarouselContent() {
        val state = FakePaywallState(
            components = listOf(loopingCarousel()),
            localizations = nonEmptyMapOf(
                LocaleId("en_US") to nonEmptyMapOf(
                    LocalizationKey("pageOneKey") to LocalizationData.Text(PAGE_ONE_TEXT),
                    LocalizationKey("pageTwoKey") to LocalizationData.Text(PAGE_TWO_TEXT),
                ),
            ),
        )
        val rootStack = state.stack as StackComponentStyle
        val carouselStyle = rootStack.children.filterIsInstance<CarouselComponentStyle>().single()

        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().height(800.dp)) {
                CarouselComponentView(style = carouselStyle, state = state, clickHandler = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Mirrors the reported repro. */
    private fun loopingCarousel() = CarouselComponent(
        pages = listOf(page("pageOneKey"), page("pageTwoKey")),
        pageAlignment = VerticalAlignment.CENTER,
        pagePeek = 28,
        loop = true,
        autoAdvance = CarouselComponent.AutoAdvancePages(
            msTimePerPage = MS_PER_PAGE,
            msTransitionTime = MS_TRANSITION,
        ),
    )

    private fun page(textKey: String) = StackComponent(
        components = listOf(
            TextComponent(
                text = LocalizationKey(textKey),
                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                size = Size(width = Fit(), height = Fit()),
            ),
        ),
        size = Size(width = Fit(), height = Fit()),
    )
}
