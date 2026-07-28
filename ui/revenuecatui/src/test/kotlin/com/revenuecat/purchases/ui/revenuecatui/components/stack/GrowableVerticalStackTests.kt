package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class GrowableVerticalStackTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var stackSize: IntSize? = null
    private val childConstraints = mutableMapOf<Int, Constraints>()
    private val childHeights = mutableMapOf<Int, Int>()

    @Test
    fun `unbounded height hands fill children their share as a minimum, not an exact size`() {
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000),
            children = listOf(style(Fit()), style(Fill)),
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                else -> ContentGrowingItem(key = 1, contentPx = 200, modifier = itemModifier)
            }
        }

        composeTestRule.runOnIdle {
            assertThat(childConstraints[1]?.minHeight).isEqualTo(900)
            assertThat(childConstraints[1]?.hasBoundedHeight).isFalse()
            // Content smaller than the share: the child stretches to fill it.
            assertThat(childHeights[1]).isEqualTo(900)
            assertThat(stackSize?.height).isEqualTo(1000)
        }
    }

    @Test
    fun `content taller than the share pushes the stack past its minimum`() {
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000),
            children = listOf(style(Fit()), style(Fill)),
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                else -> ContentGrowingItem(key = 1, contentPx = 5000, modifier = itemModifier)
            }
        }

        composeTestRule.runOnIdle {
            assertThat(childHeights[1]).isEqualTo(5000)
            assertThat(stackSize?.height).isEqualTo(5100)
        }
    }

    @Test
    fun `bounded height keeps exact weight-style shares`() {
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000, maxHeight = 1000),
            children = listOf(style(Fit()), style(Fill)),
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                else -> ContentGrowingItem(key = 1, contentPx = 5000, modifier = itemModifier)
            }
        }

        composeTestRule.runOnIdle {
            assertThat(childConstraints[1]?.minHeight).isEqualTo(900)
            assertThat(childConstraints[1]?.maxHeight).isEqualTo(900)
            // Bounded: content cannot exceed the share, matching the weight-based layout.
            assertThat(childHeights[1]).isEqualTo(900)
            assertThat(stackSize?.height).isEqualTo(1000)
        }
    }

    @Test
    fun `fill siblings share the floor equally and only tall content exceeds it`() {
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000),
            children = listOf(style(Fit()), style(Fill), style(Fill)),
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                1 -> ContentGrowingItem(key = 1, contentPx = 5000, modifier = itemModifier)
                else -> ContentGrowingItem(key = 2, contentPx = 10, modifier = itemModifier)
            }
        }

        composeTestRule.runOnIdle {
            assertThat(childConstraints[1]?.minHeight).isEqualTo(450)
            assertThat(childConstraints[2]?.minHeight).isEqualTo(450)
            assertThat(childHeights[1]).isEqualTo(5000)
            assertThat(childHeights[2]).isEqualTo(450)
            assertThat(stackSize?.height).isEqualTo(5550)
        }
    }

    @Test
    fun `nested growable stacks subdivide the floor they receive`() {
        val innerChildren = listOf(style(Fit()), style(Fill))
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000),
            children = listOf(style(Fit()), style(Fill)),
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                else -> GrowableVerticalStack(
                    children = innerChildren,
                    dimension = verticalDimension(),
                    spacing = 0.toDp(),
                    modifier = itemModifier,
                ) { innerIndex, _, innerModifier ->
                    when (innerIndex) {
                        0 -> FixedHeightItem(key = 10, heightPx = 50, modifier = innerModifier)
                        else -> ContentGrowingItem(key = 11, contentPx = 5000, modifier = innerModifier)
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            // The inner stack received a 900px floor and subdivided it: 900 - 50 fixed = 850.
            assertThat(childConstraints[11]?.minHeight).isEqualTo(850)
            assertThat(childConstraints[11]?.hasBoundedHeight).isFalse()
            assertThat(childHeights[11]).isEqualTo(5000)
            assertThat(stackSize?.height).isEqualTo(5150)
        }
    }

    @Test
    fun `spacing is reserved before computing the shares`() {
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000),
            children = listOf(style(Fit()), style(Fill)),
            spacingPx = 10,
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                else -> ContentGrowingItem(key = 1, contentPx = 200, modifier = itemModifier)
            }
        }

        composeTestRule.runOnIdle {
            assertThat(childConstraints[1]?.minHeight).isEqualTo(890)
            assertThat(stackSize?.height).isEqualTo(1000)
        }
    }

    @Test
    fun `children that compose to nothing keep the style mapping intact`() {
        setStack(
            constraints = Constraints(maxWidth = 500, minHeight = 1000),
            children = listOf(style(Fit()), style(Fit()), style(Fill)),
        ) { index, _, itemModifier ->
            when (index) {
                0 -> FixedHeightItem(key = 0, heightPx = 100, modifier = itemModifier)
                1 -> Unit // Not visible: emits no layout node.
                else -> ContentGrowingItem(key = 2, contentPx = 200, modifier = itemModifier)
            }
        }

        composeTestRule.runOnIdle {
            assertThat(childConstraints[2]?.minHeight).isEqualTo(900)
            assertThat(stackSize?.height).isEqualTo(1000)
        }
    }

    private fun setStack(
        constraints: Constraints,
        children: List<ComponentStyle>,
        spacingPx: Int = 0,
        itemContent: @Composable (index: Int, item: ComponentStyle, itemModifier: Modifier) -> Unit,
    ) {
        composeTestRule.setContent {
            Layout(
                content = {
                    GrowableVerticalStack(
                        children = children,
                        dimension = verticalDimension(),
                        spacing = spacingPx.toDp(),
                        itemContent = itemContent,
                    )
                },
            ) { measurables, _ ->
                val placeable = measurables.single().measure(constraints)
                stackSize = IntSize(placeable.width, placeable.height)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
    }

    /** Reports a fixed height regardless of available space, like a Fit or Fixed child. */
    @Composable
    private fun FixedHeightItem(key: Int, heightPx: Int, modifier: Modifier) {
        Layout(modifier = modifier) { _, constraints ->
            childConstraints[key] = constraints
            val height = heightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
            childHeights[key] = height
            layout(ITEM_WIDTH, height) {}
        }
    }

    /**
     * Fills bounded space exactly; when the maximum is unbounded, takes max(minimum, content) —
     * the same behavior as a fill-height web view with [contentPx] of reported content.
     */
    @Composable
    private fun ContentGrowingItem(key: Int, contentPx: Int, modifier: Modifier) {
        Layout(modifier = modifier) { _, constraints ->
            childConstraints[key] = constraints
            val height = if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                max(constraints.minHeight, contentPx)
            }
            childHeights[key] = height
            layout(ITEM_WIDTH, height) {}
        }
    }

    @Composable
    private fun Int.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

    private fun verticalDimension() = Dimension.Vertical(
        alignment = HorizontalAlignment.LEADING,
        distribution = FlexDistribution.START,
    )

    private fun style(height: SizeConstraint): ComponentStyle =
        previewStackComponentStyle(children = emptyList(), size = Size(width = Fit(), height = height))

    private companion object {
        const val ITEM_WIDTH = 10
    }
}
