package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toNonEmptyMapOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class TabsCarouselPageMemoryTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
    private val tabNames = listOf("a", "b")
    private val pageCount = 3

    @Test
    fun `Each tab's carousel keeps its own page when switching tabs`(): Unit = with(composeTestRule) {
        // Arrange: tab A's carousel doesn't loop, tab B's does.
        setTabsWithCarousels()

        // Act + assert: each tab returns to its own page, and B first opens on its initial page.
        swipeCarouselForward(tab = "a", fromPage = 0)
        swipeCarouselForward(tab = "a", fromPage = 1)
        onNodeWithText(tabButtonText("b")).performClick()
        assertPageShown(tab = "b", page = 0)
        swipeCarouselForward(tab = "b", fromPage = 0)

        onNodeWithText(tabButtonText("a")).performClick()
        assertPageShown(tab = "a", page = 2)
        onNodeWithText(tabButtonText("b")).performClick()
        assertPageShown(tab = "b", page = 1)
    }

    @Test
    fun `A carousel hidden mid auto-advance comes back settled on a page`(): Unit = with(composeTestRule) {
        // Arrange
        mainClock.autoAdvance = false
        setTabsWithCarousels(autoAdvanceTabA = true)
        waitForIdle()
        val restingX = pagePositionsX(tab = "a").first()

        // Act: leave tab A partway through its first auto-advance transition, then come back.
        mainClock.advanceTimeBy(AUTO_ADVANCE_MS_PER_PAGE + AUTO_ADVANCE_MS_TRANSITION / 5)
        waitForIdle()
        assertThat(pagePositionsX(tab = "a")).noneMatch { abs(it - restingX) < 1f }
        onNodeWithText(tabButtonText("b")).performClick()
        mainClock.advanceTimeBy(TAB_SWITCH_MS)
        onNodeWithText(tabButtonText("a")).performClick()
        mainClock.advanceTimeBy(TAB_SWITCH_MS)
        waitForIdle()

        // Assert: one of tab A's pages is back where a page rests, not between two.
        assertThat(pagePositionsX(tab = "a")).anyMatch { abs(it - restingX) < 1f }
    }

    private fun ComposeTestRule.pagePositionsX(tab: String): List<Float> =
        (0 until pageCount).flatMap { page ->
            onAllNodesWithText(pageText(tab, page)).fetchSemanticsNodes().map { it.positionInRoot.x }
        }

    private fun ComposeTestRule.swipeCarouselForward(tab: String, fromPage: Int) {
        // Swipe across the whole width at the height of the visible page, since the page's text node itself is
        // narrower than the pager's swipe threshold.
        val pageBounds = visiblePageBounds(pageText(tab, fromPage))
        onRoot().performTouchInput {
            swipe(
                start = Offset(x = width * SWIPE_START_FRACTION, y = pageBounds.center.y),
                end = Offset(x = width * SWIPE_END_FRACTION, y = pageBounds.center.y),
            )
        }
        waitForIdle()
    }

    private fun ComposeTestRule.assertPageShown(tab: String, page: Int) {
        visiblePageBounds(pageText(tab, page))
    }

    /**
     * The pager also composes neighboring pages off-screen (and a looping carousel composes several copies of
     * each page), so look for the one copy that lies within the viewport.
     */
    private fun ComposeTestRule.visiblePageBounds(text: String): Rect {
        val rootWidth = onRoot().fetchSemanticsNode().size.width
        val visible = onAllNodesWithText(text).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .filter { it.left >= 0f && it.right <= rootWidth && it.width > 0f }
        assertThat(visible).describedAs("visible nodes with text '%s'", text).hasSize(1)
        return visible.single()
    }

    private fun ComposeContentTestRule.setTabsWithCarousels(autoAdvanceTabA: Boolean = false) {
        val tabControlButtons = tabNames.mapIndexed { index, tab ->
            TabControlButtonComponent(
                tabIndex = index,
                tabId = tab,
                stack = StackComponent(
                    components = listOf(TextComponent(text = tabButtonKey(tab), color = textColor)),
                ),
            )
        }
        val tabsComponent = TabsComponent(
            tabs = tabNames.map { tab ->
                TabsComponent.Tab(
                    id = tab,
                    stack = StackComponent(
                        components = listOf(
                            TabControlComponent,
                            CarouselComponent(
                                pages = (0 until pageCount).map { page ->
                                    StackComponent(
                                        components = listOf(
                                            // Fill width so a swipe on the text spans the whole page.
                                            TextComponent(
                                                text = pageKey(tab, page),
                                                color = textColor,
                                                size = Size(width = Fill(), height = Fit()),
                                            ),
                                        ),
                                        size = Size(width = Fill(), height = Fit()),
                                    )
                                },
                                pageAlignment = VerticalAlignment.CENTER,
                                size = Size(width = Fill(), height = Fit()),
                                loop = tab == "b",
                                autoAdvance = if (autoAdvanceTabA && tab == "a") {
                                    CarouselComponent.AutoAdvancePages(
                                        msTimePerPage = AUTO_ADVANCE_MS_PER_PAGE.toInt(),
                                        msTransitionTime = AUTO_ADVANCE_MS_TRANSITION.toInt(),
                                        transitionType = CarouselComponent.AutoAdvancePages.TransitionType.SLIDE,
                                    )
                                } else {
                                    null
                                },
                            ),
                        ),
                    ),
                )
            },
            control = TabsComponent.TabControl.Buttons(stack = StackComponent(components = tabControlButtons)),
        )
        val localizations = tabNames
            .flatMap { tab ->
                listOf(tabButtonKey(tab) to LocalizationData.Text(tabButtonText(tab))) +
                    (0 until pageCount).map { page -> pageKey(tab, page) to LocalizationData.Text(pageText(tab, page)) }
            }
            .toMap()
            .toNonEmptyMapOrNull()!!
        val state = FakePaywallState(
            components = listOf(tabsComponent),
            localizations = nonEmptyMapOf(LocaleId("en_US") to localizations),
        )
        val tabsComponentStyle = (state.stack as StackComponentStyle).children
            .filterIsInstance<TabsComponentStyle>()
            .single()

        setContent { TabsComponentView(style = tabsComponentStyle, state = state, clickHandler = { }) }
    }

    private companion object {
        const val SWIPE_START_FRACTION = 0.9f
        const val SWIPE_END_FRACTION = 0.1f
        const val AUTO_ADVANCE_MS_PER_PAGE = 5_000L
        const val AUTO_ADVANCE_MS_TRANSITION = 1_000L
        const val TAB_SWITCH_MS = 400L
    }

    private fun tabButtonKey(tab: String) = LocalizationKey("tab_$tab")
    private fun tabButtonText(tab: String) = "Tab $tab"
    private fun pageKey(tab: String, page: Int) = LocalizationKey("tab_${tab}_page_$page")
    private fun pageText(tab: String, page: Int) = "Tab $tab page $page"
}
