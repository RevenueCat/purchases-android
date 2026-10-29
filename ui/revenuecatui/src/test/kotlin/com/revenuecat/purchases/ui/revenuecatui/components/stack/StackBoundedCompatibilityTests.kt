package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w500dp-h1000dp")
internal class StackBoundedCompatibilityTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `bounded layouts retain Row and Column geometry across distribution alignment weighting and RTL`() {
        val patterns = listOf(
            listOf(Fixed(20u), Fixed(30u), Fixed(40u)),
            listOf(Fill(), Fill(), Fill()),
            listOf(Fixed(20u), Fill(), Fixed(40u)),
            listOf(Fit(), Fill(), Fit()),
        )
        val scenarios = listOf(false, true).flatMap { horizontal ->
            LayoutDirection.values().flatMap { direction ->
                FlexDistribution.values().flatMap { distribution ->
                    (0..2).flatMap { alignment ->
                        patterns.flatMap { pattern ->
                            listOf(Fill(), Fit(), Fixed(120u)).map { rootMain ->
                                Scenario(horizontal, direction, distribution, alignment, pattern, rootMain)
                            }
                        }
                    }
                }
            }
        }
        var current by mutableStateOf(0)
        composeTestRule.setContent {
            val scenario = scenarios[current]
            key(current) {
                Row {
                    listOf(true, false).forEach { reference ->
                        val tag = if (reference) "reference" else "actual"
                        CompositionLocalProvider(LocalLayoutDirection provides scenario.direction) {
                            Box(Modifier.requiredSize(200.dp, 400.dp).testTag(tag)) {
                                Render(scenario, reference, tag)
                            }
                        }
                    }
                }
            }
        }
        val failures = mutableListOf<String>()
        scenarios.forEachIndexed { index, scenario ->
            composeTestRule.runOnIdle { current = index }
            val reference = childBounds("reference")
            val actual = childBounds("actual")
            if (reference != actual) {
                failures += "$scenario: reference=$reference actual=$actual"
            }
        }
        assertThat(failures).isEmpty()
    }

    @Test
    fun `bounded Z stacks retain Box geometry across every alignment sizing and RTL`() {
        val full = Size(Fill(), Fill())
        val fit = Size(Fit(), Fit())
        val fixed = Size(Fixed(80u), Fixed(90u))
        val scenarios = LayoutDirection.values().flatMap { direction ->
            TwoDimensionalAlignment.values().flatMap { alignment ->
                listOf(full, fit, Size(Fixed(120u), Fixed(160u))).flatMap { root ->
                    listOf(listOf(fit, fit, fit), listOf(fixed, fixed, fixed), listOf(full, fit, fixed)).map { children ->
                        LayerScenario(direction, alignment, root, children)
                    }
                }
            }
        }
        var current by mutableStateOf(0)
        composeTestRule.setContent {
            val scenario = scenarios[current]
            key(current) {
                val items = scenario.children.map { size ->
                    StyleFactory().create(StackComponent(components = emptyList(), size = size)).getOrThrow().componentStyle
                }
                Row {
                    listOf(true, false).forEach { reference ->
                        val tag = if (reference) "reference" else "actual"
                        CompositionLocalProvider(LocalLayoutDirection provides scenario.direction) {
                            Box(Modifier.requiredSize(200.dp, 400.dp).testTag(tag)) {
                                val content: @Composable (ComponentStyle, Modifier) -> Unit = { item, modifier ->
                                    val index = items.indexOfFirst { it === item }
                                    Box(modifier.size(item.size).testTag("$tag-$index")) {
                                        Box(Modifier.requiredSize(9.dp, 11.dp))
                                    }
                                }
                                if (reference) {
                                    Box(Modifier.size(scenario.root), contentAlignment = scenario.alignment.toAlignment()) {
                                        items.forEach { content(it, Modifier) }
                                    }
                                } else {
                                    ZLayerStack(items, scenario.alignment.toAlignment(), Modifier.size(scenario.root), content)
                                }
                            }
                        }
                    }
                }
            }
        }
        val failures = mutableListOf<String>()
        scenarios.forEachIndexed { index, scenario ->
            composeTestRule.runOnIdle { current = index }
            val reference = childBounds("reference")
            val actual = childBounds("actual")
            if (reference != actual) {
                failures += "$scenario: reference=$reference actual=$actual"
            }
        }
        assertThat(failures).isEmpty()
    }

    private fun childBounds(prefix: String): List<Float> {
        val parent = composeTestRule.onNodeWithTag(prefix).fetchSemanticsNode().boundsInRoot
        return (0..2).flatMap { index ->
            val child = composeTestRule.onNodeWithTag("$prefix-$index").fetchSemanticsNode().boundsInRoot
            listOf(child.left - parent.left, child.top - parent.top, child.width, child.height)
        }
    }

    @Composable
    private fun Render(scenario: Scenario, reference: Boolean, prefix: String) {
        val factory = StyleFactory()
        val items = scenario.pattern.map { main ->
            factory.create(
                StackComponent(
                    components = emptyList(),
                    size = if (scenario.horizontal) Size(main, Fill()) else Size(Fill(), main),
                ),
            ).getOrThrow().componentStyle
        }
        val rootSize = if (scenario.horizontal) Size(scenario.rootMain, Fill()) else Size(Fill(), scenario.rootMain)
        val itemContent: @Composable (Int, ComponentStyle, Modifier) -> Unit = { index, item, modifier ->
            Box(modifier.size(item.size).testTag("$prefix-$index")) {
                Box(Modifier.requiredSize(9.dp, 11.dp))
            }
        }
        if (scenario.horizontal) {
            val alignment = listOf(VerticalAlignment.TOP, VerticalAlignment.CENTER, VerticalAlignment.BOTTOM)[scenario.alignment]
            val dimension = Dimension.Horizontal(alignment, scenario.distribution)
            if (reference) {
                referenceHorizontalStack(rootSize, dimension, 7.dp, items, false, Modifier.size(rootSize), itemContent)
            } else {
                HorizontalStack(rootSize, dimension, 7.dp, items, false, Modifier.size(rootSize), itemContent)
            }
        } else {
            val alignment = listOf(HorizontalAlignment.LEADING, HorizontalAlignment.CENTER, HorizontalAlignment.TRAILING)[scenario.alignment]
            val dimension = Dimension.Vertical(alignment, scenario.distribution)
            if (reference) {
                referenceVerticalStack(rootSize, dimension, 7.dp, items, false, Modifier.size(rootSize), itemContent)
            } else {
                VerticalStack(rootSize, dimension, 7.dp, items, false, Modifier.size(rootSize), itemContent)
            }
        }
    }

    private data class Scenario(
        val horizontal: Boolean,
        val direction: LayoutDirection,
        val distribution: FlexDistribution,
        val alignment: Int,
        val pattern: List<SizeConstraint>,
        val rootMain: SizeConstraint,
    )

    private data class LayerScenario(
        val direction: LayoutDirection,
        val alignment: TwoDimensionalAlignment,
        val root: Size,
        val children: List<Size>,
    )
}
