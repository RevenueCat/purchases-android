package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class SplitFooterScrollTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Z-layer split footer scrolls vertically by default`() {
        setFooterContent(overflow = null, ruleOverflow = null)
        assertVerticalScroll()
    }

    @Test
    fun `Z-layer split footer scrolls vertically with explicit overflow`() {
        setFooterContent(overflow = StackComponent.Overflow.SCROLL, ruleOverflow = null)
        assertVerticalScroll()
    }

    @Test
    fun `width rule preserves default Z-layer footer scrolling across resizes`() {
        val state = setFooterContent(overflow = null, ruleOverflow = StackComponent.Overflow.SCROLL)
        assertVerticalScroll()
        resize(state, width = 800)
        assertVerticalScroll()
        resize(state, width = 390)
        assertVerticalScroll()
    }

    @Test
    fun `width rule enables Z-layer footer scrolling from explicit no-scroll`() {
        val state = setFooterContent(
            overflow = StackComponent.Overflow.NONE,
            ruleOverflow = StackComponent.Overflow.SCROLL,
        )
        assertNoVerticalScroll()
        resize(state, width = 800)
        assertVerticalScroll()
        resize(state, width = 390)
        assertNoVerticalScroll()
    }

    @Test
    fun `width rule disables Z-layer footer scrolling and restores it when unmatched`() {
        val state = setFooterContent(
            overflow = StackComponent.Overflow.SCROLL,
            ruleOverflow = StackComponent.Overflow.NONE,
        )
        assertVerticalScroll()
        resize(state, width = 800)
        assertNoVerticalScroll()
        resize(state, width = 390)
        assertVerticalScroll()
    }

    @Test
    fun `explicit horizontal scrolling from a rule stays horizontal`() {
        val state = setFooterContent(
            overflow = null,
            ruleOverflow = StackComponent.Overflow.SCROLL,
            dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
        )
        resize(state, width = 800)
        assertNoVerticalScroll()
        val nodes = composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange),
        ).fetchSemanticsNodes()
        assertThat(nodes).hasSize(1)
        assertThat(nodes.single().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue()).isGreaterThan(0f)
    }

    private fun assertVerticalScroll() {
        val nodes = composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        ).fetchSemanticsNodes()
        assertThat(nodes).hasSize(1)
        assertThat(nodes.single().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()).isGreaterThan(0f)
    }

    private fun assertNoVerticalScroll() {
        assertThat(composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        ).fetchSemanticsNodes()).isEmpty()
    }

    private fun resize(state: PaywallState.Loaded.Components, width: Int) {
        composeTestRule.runOnIdle { state.paywallBoundsDp = DpSize(width.dp, 400.dp) }
    }

    private fun setFooterContent(
        overflow: StackComponent.Overflow?,
        ruleOverflow: StackComponent.Overflow?,
        dimension: Dimension = Dimension.ZLayer(TwoDimensionalAlignment.CENTER),
    ): PaywallState.Loaded.Components {
        val state = FakePaywallState(stickyFooter = StickyFooterComponent(
            stack = StackComponent(
                components = listOf(StackComponent(
                    components = emptyList(),
                    size = if (dimension is Dimension.Horizontal) {
                        Size(width = Fixed(1600u), height = Fixed(80u))
                    } else {
                        Size(width = Fill(), height = Fixed(1600u))
                    },
                )),
                dimension = dimension,
                overflow = overflow,
                overrides = ruleOverflow?.let {
                    listOf(ComponentOverride(
                        conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                            operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                            value = 600.0,
                        )),
                        properties = PartialStackComponent(overflow = it),
                    ))
                }.orEmpty(),
            ),
            layout = StickyFooterComponent.Layout.SPLIT_TRAILING,
        ))
        state.paywallBoundsDp = DpSize(390.dp, 400.dp)
        composeTestRule.setContent {
            Box(Modifier.size(390.dp, 400.dp)) {
                ComponentView(style = checkNotNull(state.stickyFooter), state = state, onClick = {})
            }
        }
        return state
    }
}
