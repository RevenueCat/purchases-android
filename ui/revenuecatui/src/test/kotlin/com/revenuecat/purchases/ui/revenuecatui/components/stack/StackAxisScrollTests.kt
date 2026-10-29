package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w500dp-h1000dp")
internal class StackAxisScrollTests(private val case: Case) {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `sizing scrolling and native taps work across nested Z layers scroll axes and RTL`() {
        val vertical = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START)
        val horizontal = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START)
        val chains = (1..3).map { depth ->
            List(depth) { Dimension.ZLayer(TwoDimensionalAlignment.CENTER) }
        }
        val factory = StyleFactory()
        val scenarios = Orientation.values().flatMap { scroll ->
            LayoutDirection.values().flatMap { direction ->
                (if (case.overflow == null) chains else chains.take(1)).map { chain ->
                    fun size(main: SizeConstraint) =
                        if (scroll == Orientation.Vertical) Size(Fill(), main) else Size(main, Fill())
                    var component: PaywallComponent = if (case.overflow != null) {
                        StackComponent(
                            components = listOf(
                                StackComponent(components = emptyList(), size = size(Fixed(650u))),
                                button(size(Fixed(80u))),
                            ),
                            dimension = if (scroll == Orientation.Vertical) vertical else horizontal,
                            size = size(case.overflow),
                        )
                    } else {
                        button(case.size)
                    }
                    chain.asReversed().forEachIndexed { index, axis ->
                        component = StackComponent(
                            components = listOf(component), dimension = axis, size = Size(Fill(), Fill()),
                            padding = if (index == chain.lastIndex) {
                                Padding(case.padding, case.padding, case.padding, case.padding)
                            } else Padding(),
                        )
                    }
                    Scenario(scroll, direction, chain, factory.create(component).getOrThrow().componentStyle as StackComponentStyle)
                }
            }
        }
        var current by mutableStateOf(0)
        var clicks = 0
        val state = FakePaywallState(components = emptyList())
        rule.setContent {
            val scenario = scenarios[current]
            key(current) {
                CompositionLocalProvider(LocalLayoutDirection provides scenario.direction) {
                    Box(Modifier.requiredSize(200.dp, 400.dp)) {
                        val scrollState = rememberScrollState()
                        StackComponentView(
                            style = scenario.style, state = state, clickHandler = { clicks++ },
                            ancestorScrollOrientations = setOf(scenario.scroll),
                            modifier = Modifier.fillMaxSize().then(
                                if (scenario.scroll == Orientation.Vertical) Modifier.verticalScroll(scrollState)
                                else Modifier.horizontalScroll(scrollState),
                            ),
                        )
                    }
                }
            }
        }
        val failures = mutableListOf<String>()
        scenarios.forEachIndexed { index, scenario ->
            rule.runOnIdle { current = index }
            try {
                val node = rule.onNode(hasClickAction())
                if (case.overflow == null) {
                    node.assertWidthIsEqualTo(case.width.dp).assertHeightIsEqualTo(case.height.dp)
                } else {
                    node.performScrollTo().assertIsDisplayed()
                }
                val before = clicks
                node.performTouchInput { click(center) }
                rule.runOnIdle { assertThat(clicks).isEqualTo(before + 1) }
            } catch (failure: AssertionError) {
                failures += "${scenario.scroll}/${scenario.direction}/${scenario.chain}: ${failure.message}"
            }
        }
        assertThat(failures).isEmpty()
    }

    private fun button(size: Size) = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(
            components = if (size.width is Fit || size.height is Fit) {
                listOf(StackComponent(components = emptyList(), size = Size(Fixed(30u), Fixed(40u))))
            } else emptyList(),
            size = size,
            overrides = case.overrideSize?.let { listOf(ComponentOverride(emptyList(), PartialStackComponent(size = it))) }.orEmpty(),
        ),
    )

    private data class Scenario(
        val scroll: Orientation,
        val direction: LayoutDirection,
        val chain: List<Dimension>,
        val style: StackComponentStyle,
    )

    data class Case(
        val name: String,
        val size: Size = Size(Fill(), Fill()),
        val width: Int = 200,
        val height: Int = 400,
        val padding: Double = 0.0,
        val overrideSize: Size? = null,
        val overflow: SizeConstraint? = null,
    ) {
        override fun toString() = name
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf(
            Case("Fill"),
            Case("capped Fill", Size(Fill(max = 80u), Fill(max = 90u)), 80, 90),
            Case("Fixed", Size(Fixed(80u), Fixed(90u)), 80, 90),
            Case("Fit", Size(Fit(), Fit()), 30, 40),
            Case("padding", width = 160, height = 360, padding = 20.0),
            Case("minimum", Size(Fill(min = 80u), Fill(min = 90u))),
            Case("minimum wins maximum", Size(Fill(min = 80u, max = 60u), Fill(min = 90u, max = 60u)), 80, 90),
            Case("Fill to Fixed override", width = 80, height = 90, overrideSize = Size(Fixed(80u), Fixed(90u))),
            Case("Fixed to Fill override", Size(Fixed(80u), Fixed(90u)), overrideSize = Size(Fill(), Fill())),
            Case("Fixed width Fill height", Size(Fixed(80u), Fill()), 80, 400),
            Case("Fill width Fixed height", Size(Fill(), Fixed(90u)), 200, 90),
            Case("Fit width Fill height", Size(Fit(), Fill()), 30, 400),
            Case("Fill width Fit height", Size(Fill(), Fit()), 200, 40),
            Case("oversized Fit", overflow = Fit()),
            Case("oversized Fill", overflow = Fill()),
        ).map { arrayOf(it) }
    }
}
