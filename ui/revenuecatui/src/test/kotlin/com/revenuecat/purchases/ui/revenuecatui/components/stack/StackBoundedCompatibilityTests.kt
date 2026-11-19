package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
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
    fun `bounded rows and columns retain native geometry across sizing distribution alignment and RTL`() {
        val patterns = listOf(
            listOf(Fixed(20u), Fixed(30u), Fixed(40u)), listOf(Fill(), Fill(), Fill()),
            listOf(Fixed(20u), Fill(), Fixed(40u)), listOf(Fit(), Fill(), Fit()),
        )
        compare(linearCases(patterns, listOf(Fill(), Fit(), Fixed(120u))))
    }

    @Test
    fun `bounded spacers retain rounding and shrink when children exhaust the available space`() {
        compare(
            linearCases(
                listOf(listOf(Fixed(20u), Fixed(30u), Fixed(40u)), listOf(Fixed(600u), Fixed(20u), Fixed(40u))),
                listOf(Fill(), Fixed(100u), Fixed(201u)),
            ),
            width = 201, height = 401,
        )
    }

    private fun linearCases(patterns: List<List<SizeConstraint>>, roots: List<SizeConstraint>) =
        listOf(false, true).flatMap { horizontal ->
            fun size(main: SizeConstraint) = if (horizontal) Size(main, Fill()) else Size(Fill(), main)
            LayoutDirection.values().flatMap { direction ->
                FlexDistribution.values().flatMap { distribution ->
                    (0..2).flatMap { alignment ->
                        val dimension = if (horizontal) {
                            Dimension.Horizontal(VerticalAlignment.values()[alignment], distribution)
                        } else Dimension.Vertical(HorizontalAlignment.values()[alignment], distribution)
                        patterns.flatMap { pattern ->
                            roots.map { root ->
                                Case(direction, dimension, size(root), pattern.map(::size))
                            }
                        }
                    }
                }
            }
        }

    @Test
    fun `small remaining space never creates a negative edge spacer`() {
        compare(LayoutDirection.values().map { direction ->
            Case(
                direction, Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.SPACE_AROUND),
                Size(Fixed(103u), Fill()), List(4) { Size(Fixed(20u), Fill()) },
            )
        })
    }

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
        when (val dimension = case.dimension) {
            is Dimension.Horizontal -> if (reference) ReferenceLinear(case, items, content) else {
                HorizontalStack(case.root, dimension, 7.dp, items, false, modifier, content)
            }
            is Dimension.Vertical -> if (reference) ReferenceLinear(case, items, content) else {
                VerticalStack(case.root, dimension, 7.dp, items, false, modifier, content)
            }
            is Dimension.ZLayer -> {
                val layers: @Composable (ComponentStyle, Modifier) -> Unit = { item, childModifier ->
                    content(items.indexOfFirst { it === item }, item, childModifier)
                }
                if (reference) {
                    Box(modifier, contentAlignment = dimension.alignment.toAlignment()) { items.forEach { layers(it, Modifier) } }
                } else ZLayerStack(items, dimension.alignment.toAlignment(), modifier, layers)
            }
        }
    }

    // Native Row/Column with the previous stack's weights and SPACE_* spacers, independent of the custom layout.
    @Composable
    private fun ReferenceLinear(
        case: Case,
        items: List<ComponentStyle>,
        content: @Composable (Int, ComponentStyle, Modifier) -> Unit,
    ) {
        val horizontal = case.dimension is Dimension.Horizontal
        val distribution = when (val dimension = case.dimension) {
            is Dimension.Horizontal -> dimension.distribution
            is Dimension.Vertical -> dimension.distribution
            else -> error("Expected a row or column")
        }
        fun main(size: Size) = if (horizontal) size.width else size.height
        val spread = main(case.root) !is Fit && items.none { main(it.size) is Fill }
        val edges = spread && distribution in listOf(FlexDistribution.SPACE_AROUND, FlexDistribution.SPACE_EVENLY)
        val children: @Composable ((Float) -> Modifier, Modifier) -> Unit = { weight, gap ->
            if (edges) Spacer(weight(1f))
            items.forEachIndexed { index, item ->
                content(index, item, if (main(item.size) is Fill) weight(1f) else Modifier)
                if (distribution.usesAllAvailableSpace && index != items.lastIndex) {
                    Spacer(gap)
                    if (spread) Spacer(weight(if (distribution == FlexDistribution.SPACE_AROUND) 2f else 1f))
                }
            }
            if (edges) Spacer(weight(1f))
        }
        when (val dimension = case.dimension) {
            is Dimension.Horizontal -> Row(
                Modifier.size(case.root), dimension.distribution.toHorizontalArrangement(7.dp), dimension.alignment.toAlignment(),
            ) { children({ Modifier.weight(it) }, Modifier.widthIn(min = 7.dp)) }
            is Dimension.Vertical -> Column(
                Modifier.size(case.root), dimension.distribution.toVerticalArrangement(7.dp), dimension.alignment.toAlignment(),
            ) { children({ Modifier.weight(it) }, Modifier.heightIn(min = 7.dp)) }
            else -> error("Expected a row or column")
        }
    }

    private data class Case(
        val direction: LayoutDirection,
        val dimension: Dimension,
        val root: Size,
        val children: List<Size>,
    )
}
