package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class StickyFooterSizeTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val actions = mutableListOf<PaywallAction.External>()
    private val annual = TestData.Packages.annual
    private val monthly = TestData.Packages.monthly

    @Test
    fun `fit footer stays compact when deselecting a package changes its height to fill`() {
        val state = showPackageRow(rowHeight = Fit())

        assertFooterHeight(68, state.footerHeightPx)
        composeTestRule.onNode(isNotSelected()).performClick()
        composeTestRule.waitForIdle()
        assertFooterHeight(68, state.footerHeightPx)
        composeTestRule.onNode(hasText("dummyText") and hasClickAction())
            .assertIsDisplayed()
            .assertHeightIsEqualTo(44.dp)
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
            assertThat(actions.single()).isInstanceOf(PaywallAction.External.PurchasePackage::class.java)
        }
    }

    @Test
    fun `explicit row height is honored inside a fit footer`() {
        val state = showPackageRow(rowHeight = Fixed(80u))

        composeTestRule.onNode(isNotSelected()).performClick()
        composeTestRule.waitForIdle()
        assertFooterHeight(124, state.footerHeightPx)
        composeTestRule.onNode(isNotSelected())
            .assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun `a fill child in a vertical fit footer wraps its content`() {
        val fillBlock = StackComponent(listOf(block(24u)), size = Size(Fill(), Fill()))
        val state = showFooter(StackComponent(listOf(fillBlock, purchaseButton())))

        assertFooterHeight(68, state.footerHeightPx)
        composeTestRule.onNodeWithText("dummyText").assertIsDisplayed()
    }

    @Test
    fun `the purchase button in an oversized fit footer can scroll into view`() {
        showFooter(StackComponent(listOf(block(450u), purchaseButton())))

        composeTestRule.onNodeWithText("dummyText").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a scrolling child inside a fixed height footer row retains its scroll range`() {
        val scrollingChild = StackComponent(listOf(block(180u)), overflow = StackComponent.Overflow.SCROLL)
        val fixedRow = StackComponent(listOf(scrollingChild), size = Size(Fill(), Fixed(80u)))
        showFooter(StackComponent(listOf(fixedRow, purchaseButton())))

        val verticalScroll = SemanticsProperties.VerticalScrollAxisRange
        val ranges = composeTestRule.onAllNodes(SemanticsMatcher.keyIsDefined(verticalScroll))
            .fetchSemanticsNodes().map { it.config[verticalScroll].maxValue() }
        assertThat(ranges).contains(with(composeTestRule.density) { 100.dp.toPx() })
    }

    @Test
    fun `a page inside a fixed height carousel in a fit footer can scroll to its purchase button`() {
        val page = StackComponent(listOf(block(180u), purchaseButton()), overflow = StackComponent.Overflow.SCROLL)
        val carousel = CarouselComponent(
            pages = listOf(page),
            pageAlignment = VerticalAlignment.CENTER,
            size = Size(Fill(), Fixed(80u)),
        )
        val state = showFooter(StackComponent(listOf(carousel)))

        assertFooterHeight(80, state.footerHeightPx)
        composeTestRule.onNodeWithText("dummyText").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a fixed height footer retains its declared height`() {
        val state = showFooter(StackComponent(listOf(block(24u)), size = Size(Fill(), Fixed(100u))))

        assertFooterHeight(100, state.footerHeightPx)
    }

    @Test
    fun `oversized footer respects an explicit scroll opt out`() {
        showFooter(StackComponent(listOf(block(450u), purchaseButton()), overflow = StackComponent.Overflow.NONE))

        val range = SemanticsProperties.VerticalScrollAxisRange
        val scrollable = composeTestRule.onAllNodes(SemanticsMatcher.keyIsDefined(range)).fetchSemanticsNodes()
        assertThat(scrollable.none { it.config[range].maxValue() > 0f }).isTrue()
    }

    private fun purchaseButton() = PurchaseButtonComponent(
        StackComponent(
            listOf(TextComponent(LocalizationKey("dummyKey"), color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())))),
            size = Size(Fill(), Fixed(44u)),
        ),
    )

    private fun showPackageRow(rowHeight: SizeConstraint): PaywallState.Loaded.Components {
        val card = PackageComponent(
            packageId = annual.identifier,
            isSelectedByDefault = true,
            stack = StackComponent(
                components = listOf(block(24u)),
                size = Size(Fill(), Fill()),
                overrides = listOf(
                    ComponentOverride(
                        conditions = listOf(ComponentOverride.Condition.Selected),
                        properties = PartialStackComponent(size = Size(Fill(), Fit())),
                    ),
                ),
            ),
        )
        val row = StackComponent(
            components = listOf(
                card,
                PackageComponent(monthly.identifier, isSelectedByDefault = false, stack = block(24u)),
            ),
            dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
            size = Size(Fill(), rowHeight),
        )
        return showFooter(StackComponent(listOf(row, purchaseButton())))
    }

    private fun showFooter(stack: StackComponent): PaywallState.Loaded.Components {
        val state = FakePaywallState(
            packages = listOf(annual, monthly),
            stickyFooter = StickyFooterComponent(stack),
            addPackageComponents = false,
        )
        composeTestRule.setContent {
            Box(Modifier.size(300.dp, 400.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                LoadedPaywallComponents(state, clickHandler = { actions.add(it) })
            }
        }
        composeTestRule.waitForIdle()
        return state
    }

    private fun block(height: UInt) = StackComponent(emptyList(), size = Size(Fill(), Fixed(height)))

    private fun assertFooterHeight(expectedDp: Int, actualPx: Int) {
        assertThat(actualPx).isEqualTo(with(composeTestRule.density) { expectedDp.dp.roundToPx() })
    }
}
