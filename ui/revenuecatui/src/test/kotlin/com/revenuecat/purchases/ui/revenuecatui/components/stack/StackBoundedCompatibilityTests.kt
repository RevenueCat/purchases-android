package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
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
    @get:Rule val rule = createComposeRule()

    @Test
    fun `bounded Z stacks retain Box geometry across sizing alignment and RTL`() {
        val full = Size(Fill(), Fill())
        val fit = Size(Fit(), Fit())
        val fixed = Size(Fixed(80u), Fixed(90u))
        compare(LayoutDirection.values().flatMap { direction ->
            TwoDimensionalAlignment.values().flatMap { alignment ->
                listOf(full, fit, Size(Fixed(120u), Fixed(160u))).flatMap { root ->
                    listOf(listOf(fit, fit, fit), listOf(fixed, fixed, fixed), listOf(full, fit, fixed)).map { children ->
                        Case(direction, Dimension.ZLayer(alignment), root, children)
                    }
                }
            }
        })
    }

    private fun compare(cases: List<Case>, width: Int = 200, height: Int = 400) {
        var current by mutableStateOf(0)
        rule.setContent {
            val case = cases[current]
            key(current) {
                CompositionLocalProvider(LocalLayoutDirection provides case.direction) {
                    Row {
                        listOf(true, false).forEach { reference ->
                            val tag = if (reference) "reference" else "actual"
                            Box(Modifier.requiredSize(width.dp, height.dp).testTag(tag)) { Render(case, reference, tag) }
                        }
                    }
                }
            }
        }
        cases.forEachIndexed { index, case ->
            rule.runOnIdle { current = index }
            assertThat(childBounds("actual", case.children.size)).describedAs(case.toString())
                .isEqualTo(childBounds("reference", case.children.size))
        }
    }

    private fun childBounds(prefix: String, count: Int): List<Float> {
        val parent = rule.onNodeWithTag(prefix).fetchSemanticsNode().boundsInRoot
        return (0 until count).flatMap { index ->
            val child = rule.onNodeWithTag("$prefix-$index").fetchSemanticsNode().boundsInRoot
            if (child.width == 0f || child.height == 0f) listOf(0f, 0f, 0f, 0f)
            else listOf(child.left - parent.left, child.top - parent.top, child.width, child.height)
        }
    }

    @Composable
    private fun Render(case: Case, reference: Boolean, prefix: String) {
        val factory = StyleFactory()
        val items = case.children.map { factory.create(StackComponent(components = emptyList(), size = it)).getOrThrow().componentStyle }
        val content: @Composable (Int, ComponentStyle, Modifier) -> Unit = { index, item, modifier ->
            Box(modifier.size(item.size).testTag("$prefix-$index")) { Box(Modifier.requiredSize(9.dp, 11.dp)) }
        }
        val modifier = Modifier.size(case.root)
        val dimension = case.dimension as Dimension.ZLayer
        val layers: @Composable (ComponentStyle, Modifier) -> Unit = { item, childModifier ->
            content(items.indexOfFirst { it === item }, item, childModifier)
        }
        if (reference) {
            Box(modifier, contentAlignment = dimension.alignment.toAlignment()) { items.forEach { layers(it, Modifier) } }
        } else ZLayerStack(items, dimension.alignment.toAlignment(), modifier, layers)
    }

    private data class Case(
        val direction: LayoutDirection,
        val dimension: Dimension,
        val root: Size,
        val children: List<Size>,
    )
}
