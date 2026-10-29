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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
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
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w500dp-h1000dp")
internal class StackAxisOverflowTests {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `oversized Fit content remains scrollable and its final button works for all root axes and RTL`() {
        runOverflowMatrix(Fit())
    }

    @Test
    fun `oversized Fill content remains scrollable and its final button works for all root axes and RTL`() {
        runOverflowMatrix(Fill())
    }

    private fun runOverflowMatrix(contentSize: SizeConstraint) {
        val vertical = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START)
        val horizontal = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START)
        val factory = StyleFactory()
        val cases = Orientation.values().flatMap { scroll ->
            LayoutDirection.values().flatMap { direction ->
                listOf(vertical, horizontal, Dimension.ZLayer(TwoDimensionalAlignment.CENTER)).map { root ->
                    fun size(main: SizeConstraint) =
                        if (scroll == Orientation.Vertical) Size(Fill(), main) else Size(main, Fill())
                    val content = StackComponent(
                        components = listOf(
                            StackComponent(components = emptyList(), size = size(Fixed(650u))),
                            ButtonComponent(
                                action = ButtonComponent.Action.NavigateBack,
                                stack = StackComponent(components = emptyList(), size = size(Fixed(80u))),
                            ),
                        ),
                        dimension = if (scroll == Orientation.Vertical) vertical else horizontal,
                        size = size(contentSize),
                    )
                    val stack = StackComponent(components = listOf(content), dimension = root, size = Size(Fill(), Fill()))
                    Case(scroll, direction, root, factory.create(stack).getOrThrow().componentStyle as StackComponentStyle)
                }
            }
        }
        var current by mutableStateOf(0)
        var clicks = 0
        val state = FakePaywallState(components = emptyList())
        rule.setContent {
            val case = cases[current]
            key(current) {
                CompositionLocalProvider(LocalLayoutDirection provides case.direction) {
                    Box(Modifier.requiredSize(200.dp, 400.dp)) {
                        val scrollState = rememberScrollState()
                        StackComponentView(
                            style = case.style, state = state, clickHandler = { clicks++ },
                            ancestorScrollOrientations = setOf(case.scroll),
                            modifier = Modifier.fillMaxSize().then(
                                if (case.scroll == Orientation.Vertical) Modifier.verticalScroll(scrollState)
                                else Modifier.horizontalScroll(scrollState),
                            ),
                        )
                    }
                }
            }
        }
        cases.forEachIndexed { index, case ->
            rule.runOnIdle { current = index }
            try {
                rule.onNode(hasClickAction()).performScrollTo().assertIsDisplayed().performTouchInput { click(center) }
                rule.runOnIdle { assertThat(clicks).isEqualTo(index + 1) }
            } catch (failure: AssertionError) {
                throw AssertionError("$case: ${failure.message}", failure)
            }
        }
    }

    private data class Case(
        val scroll: Orientation,
        val direction: LayoutDirection,
        val root: Dimension,
        val style: StackComponentStyle,
    )
}
