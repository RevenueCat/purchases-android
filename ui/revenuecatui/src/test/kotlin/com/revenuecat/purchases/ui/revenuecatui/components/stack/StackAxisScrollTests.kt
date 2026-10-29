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
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w500dp-h1000dp")
internal class StackAxisScrollTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Fill tap targets fill every one to three level axis nesting in either scroll direction and layout direction`() {
        runMatrix(Size(Fill(), Fill()), 200, 400)
    }

    @Test
    fun `limited Fill tap targets retain their maximums across every axis nesting`() {
        runMatrix(Size(Fill(max = 80u), Fill(max = 90u)), 80, 90)
    }

    @Test
    fun `Fixed tap targets are not stretched across any axis nesting`() {
        runMatrix(Size(Fixed(80u), Fixed(90u)), 80, 90)
    }

    @Test
    fun `Fit tap targets retain their content size across every axis nesting`() {
        runMatrix(
            Size(Fit(), Fit()), 30, 40,
            leafChildren = listOf(StackComponent(components = emptyList(), size = Size(Fixed(30u), Fixed(40u)))),
        )
    }

    @Test
    fun `Fill tap targets use the viewport after root padding in every axis nesting`() {
        runMatrix(Size(Fill(), Fill()), 160, 360, rootPadding = Padding(20.0, 20.0, 20.0, 20.0))
    }

    @Test
    fun `Fill minimums below the viewport do not shrink any axis nesting`() {
        runMatrix(Size(Fill(min = 80u), Fill(min = 90u)), 200, 400)
    }

    @Test
    fun `Fill minimum wins over a smaller maximum in every axis nesting`() {
        runMatrix(Size(Fill(min = 80u, max = 60u), Fill(min = 90u, max = 60u)), 80, 90)
    }

    @Test
    fun `resolved Fixed overrides do not inherit Fill constraints in any axis nesting`() {
        runMatrix(
            Size(Fill(), Fill()), 80, 90,
            overrides = listOf(ComponentOverride(emptyList(), PartialStackComponent(size = Size(Fixed(80u), Fixed(90u))))),
        )
    }

    @Test
    fun `resolved Fill overrides receive viewport space in every axis nesting`() {
        runMatrix(
            Size(Fixed(80u), Fixed(90u)), 200, 400,
            overrides = listOf(ComponentOverride(emptyList(), PartialStackComponent(size = Size(Fill(), Fill())))),
        )
    }

    private fun runMatrix(
        leafSize: Size,
        expectedWidth: Int,
        expectedHeight: Int,
        leafChildren: List<PaywallComponent> = emptyList(),
        rootPadding: Padding = Padding(),
        overrides: List<ComponentOverride<PartialStackComponent>> = emptyList(),
    ) {
        val dimensions = listOf(
            Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
            Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
            Dimension.ZLayer(TwoDimensionalAlignment.CENTER),
        )
        val chains = dimensions.map { listOf(it) } +
            dimensions.flatMap { a -> dimensions.map { b -> listOf(a, b) } } +
            dimensions.flatMap { a -> dimensions.flatMap { b -> dimensions.map { c -> listOf(a, b, c) } } }
        val factory = StyleFactory()
        val scenarios = Orientation.values().flatMap { scroll ->
            LayoutDirection.values().flatMap { direction ->
                chains.map { chain ->
                    var component: PaywallComponent = ButtonComponent(
                        action = ButtonComponent.Action.NavigateBack,
                        stack = StackComponent(components = leafChildren, size = leafSize, overrides = overrides),
                    )
                    chain.asReversed().forEachIndexed { index, dimension ->
                        component = StackComponent(
                            components = listOf(component), dimension = dimension, size = Size(Fill(), Fill()),
                            padding = if (index == chain.lastIndex) rootPadding else Padding(),
                        )
                    }
                    Scenario(scroll, direction, chain, factory.create(component).getOrThrow().componentStyle as StackComponentStyle)
                }
            }
        }
        var current by mutableStateOf(0)
        var clicks = 0
        val state = FakePaywallState(components = emptyList())
        composeTestRule.setContent {
            val scenario = scenarios[current]
            key(current) {
                CompositionLocalProvider(LocalLayoutDirection provides scenario.direction) {
                    Box(Modifier.requiredSize(200.dp, 400.dp)) {
                        val scrollState = rememberScrollState()
                        StackComponentView(
                            style = scenario.style,
                            state = state,
                            clickHandler = { clicks++ },
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
            composeTestRule.runOnIdle { current = index }
            try {
                composeTestRule.onNode(hasClickAction())
                    .assertWidthIsEqualTo(expectedWidth.dp)
                    .assertHeightIsEqualTo(expectedHeight.dp)
                    .performTouchInput { click(center) }
            } catch (failure: AssertionError) {
                failures += "${scenario.scroll}/${scenario.direction}/${scenario.chain}: ${failure.message}"
            }
        }
        assertThat(failures).isEmpty()
        composeTestRule.runOnIdle { assertThat(clicks).isEqualTo(scenarios.size) }
    }

    private data class Scenario(
        val scroll: Orientation,
        val direction: LayoutDirection,
        val chain: List<Dimension>,
        val style: StackComponentStyle,
    )
}
