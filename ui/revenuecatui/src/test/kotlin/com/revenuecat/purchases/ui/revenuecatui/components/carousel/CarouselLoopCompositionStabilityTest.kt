package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
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
 * A `loop: true` carousel ran its pager over an unbounded index space, so every auto-advance
 * destroyed and rebuilt a page. Observed on device as a `web_view` page fully reloading every ~8s
 * for as long as the paywall was open.
 */
// Wide enough that the per-test host widths below are reachable; the default Robolectric screen
// would coerce them all to the same value.
@Config(sdk = [26], qualifiers = "w1200dp-h800dp")
@RunWith(AndroidJUnit4::class)
class CarouselLoopCompositionStabilityTest {

    private companion object {
        const val PAGE_ONE_TEXT = "pageOneText"
        const val PAGE_TWO_TEXT = "pageTwoText"
        const val MS_PER_PAGE = 3500
        const val MS_TRANSITION = 500
        const val PAGE_PEEK = 28
        // Two pages plus two clones per side puts each page on three ring indices. The pad is the
        // minimum here because this peek exposes one neighbour per side.
        const val EXPECTED_INSTANCES_PER_PAGE = 3
        // Against a 300dp viewport this peek leaves 80dp pages, so two neighbours show per side.
        const val WIDE_PAGE_PEEK = 110
        const val EXPECTED_WIDE_PEEK_INSTANCES_PER_PAGE = 4
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private var hostWidth by mutableStateOf<Dp?>(null)

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

    @Test
    fun `a peek wider than a page gets a clone for every page it exposes`() {
        // With the minimum pad the pager runs out of ring before the peek is filled, and the gap
        // is permanent: re-centring still leaves the outermost real page against the ring edge.
        setLoopingCarouselContent(pagePeek = WIDE_PAGE_PEEK, width = 300.dp)

        composeTestRule.onAllNodesWithText(PAGE_ONE_TEXT, useUnmergedTree = true)
            .assertCountEquals(EXPECTED_WIDE_PEEK_INSTANCES_PER_PAGE)
        composeTestRule.onAllNodesWithText(PAGE_TWO_TEXT, useUnmergedTree = true)
            .assertCountEquals(EXPECTED_WIDE_PEEK_INSTANCES_PER_PAGE)
    }

    @Test
    fun `a resize that changes the pad leaves the carousel on the same page`() {
        // Pad 3 -> 2, small enough that the current index stays inside the shorter ring: this is
        // the index-to-page mapping on its own, with no re-anchoring involved.
        setLoopingCarouselContent(pagePeek = WIDE_PAGE_PEEK, width = 300.dp, autoAdvance = false)
        val pageBeforeResize = centeredPageText()

        hostWidth = 1000.dp
        composeTestRule.waitForIdle()

        assertThat(centeredPageText()).isEqualTo(pageBeforeResize)
    }

    @Test
    fun `a resize that shrinks the ring leaves the carousel on the same page`() {
        // Pad 5 -> 2, big enough to put the current index outside the shorter ring, which is the
        // case PagerState coerces. A coerced index means a different page, so this one needs the
        // re-anchor rather than the mapping alone.
        setLoopingCarouselContent(pagePeek = WIDE_PAGE_PEEK, width = 250.dp, autoAdvance = false)
        val pageBeforeResize = centeredPageText()

        hostWidth = 1000.dp
        composeTestRule.waitForIdle()

        assertThat(centeredPageText()).isEqualTo(pageBeforeResize)
    }

    @Test
    fun `a resize that changes the pad keeps auto-advancing`() {
        // The ring grows with the pad, so an auto-advance loop holding the old ring size stops
        // targeting anything once the real zone moves past it, and never recovers.
        setLoopingCarouselContent(pagePeek = WIDE_PAGE_PEEK, width = 340.dp)

        hostWidth = 250.dp
        composeTestRule.waitForIdle()

        val centeredPages = mutableListOf(centeredPageText())
        repeat(2) {
            advanceOnePage()
            centeredPages += centeredPageText()
        }

        assertThat(centeredPages).containsExactly(PAGE_ONE_TEXT, PAGE_TWO_TEXT, PAGE_ONE_TEXT)
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

    private fun setLoopingCarouselContent(
        pagePeek: Int = PAGE_PEEK,
        width: Dp? = null,
        autoAdvance: Boolean = true,
    ) {
        hostWidth = width
        val state = FakePaywallState(
            components = listOf(loopingCarousel(pagePeek = pagePeek, autoAdvance = autoAdvance)),
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
            val currentWidth = hostWidth
            Box(
                Modifier
                    .height(800.dp)
                    .then(currentWidth?.let { Modifier.width(it) } ?: Modifier.fillMaxWidth()),
            ) {
                CarouselComponentView(style = carouselStyle, state = state, clickHandler = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Mirrors the reported repro. */
    private fun loopingCarousel(pagePeek: Int, autoAdvance: Boolean) = CarouselComponent(
        pages = listOf(page("pageOneKey"), page("pageTwoKey")),
        pageAlignment = VerticalAlignment.CENTER,
        pagePeek = pagePeek,
        loop = true,
        autoAdvance = CarouselComponent.AutoAdvancePages(
            msTimePerPage = MS_PER_PAGE,
            msTransitionTime = MS_TRANSITION,
        ).takeIf { autoAdvance },
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
