package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StackSizeConstraintTest {

    @Test
    fun `fit with positive minimum allows flex distribution`() {
        assertThat(Fit(min = 100u).allowsFlexDistribution).isTrue()
    }

    @Test
    fun `unconstrained fit does not allow flex distribution`() {
        assertThat(Fit().allowsFlexDistribution).isFalse()
        assertThat(Fit(max = 100u).allowsFlexDistribution).isFalse()
        assertThat(Fit(min = 0u).allowsFlexDistribution).isFalse()
    }

    @Test
    fun `fill and fixed allow flex distribution`() {
        assertThat(Fill().allowsFlexDistribution).isTrue()
        assertThat(Fixed(100u).allowsFlexDistribution).isTrue()
    }

    @Test
    fun `stacks without any min or max keep using Row and Column`() {
        // Every combination pre-existing paywalls can express must stay on the Row/Column path.
        val children = listOf(
            stackStyle(Size(width = Fill(), height = Fill())),
            stackStyle(Size(width = Fit(), height = Fit())),
            stackStyle(Size(width = Fixed(10u), height = Fixed(10u))),
        )
        val stackSizes = listOf(
            Size(width = Fill(), height = Fill()),
            Size(width = Fit(), height = Fit()),
            Size(width = Fixed(100u), height = Fixed(100u)),
            Size(width = Fit(max = 100u), height = Fit(max = 100u)),
        )

        for (stackSize in stackSizes) {
            for (distribution in FlexDistribution.values()) {
                for (orientation in Orientation.values()) {
                    assertThat(needsConstrainedFillLayout(stackSize, distribution, children, orientation))
                        .describedAs("size=$stackSize distribution=$distribution orientation=$orientation")
                        .isFalse()
                }
            }
        }
    }

    @Test
    fun `a child with a limited Fill on the main axis needs the constrained layout`() {
        val children = listOf(stackStyle(Size(width = Fill(max = 100u), height = Fill())))
        val stackSize = Size(width = Fill(), height = Fill())

        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.START, children, Orientation.Horizontal))
            .isTrue()
        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.START, children, Orientation.Vertical))
            .isFalse()
    }

    @Test
    fun `a child whose override introduces a limited Fill needs the constrained layout`() {
        val child = stackStyle(
            size = Size(width = Fill(), height = Fill()),
            overrides = listOf(
                PresentedOverride(
                    conditions = emptyList(),
                    properties = PresentedStackPartial(
                        backgroundStyles = null,
                        borderStyles = null,
                        shadowStyles = null,
                        badgeStyle = null,
                        partial = PartialStackComponent(size = Size(width = Fill(), height = Fill(min = 20u))),
                    ),
                ),
            ),
        )
        val stackSize = Size(width = Fill(), height = Fill())

        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.START, listOf(child), Orientation.Vertical))
            .isTrue()
        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.START, listOf(child), Orientation.Horizontal))
            .isFalse()
    }

    @Test
    fun `a Fit stack with a positive minimum needs the constrained layout only when Row or Column would expand it`() {
        val stackSize = Size(width = Fit(min = 100u), height = Fit())
        val fillChild = listOf(stackStyle(Size(width = Fill(), height = Fill())))
        val fixedChild = listOf(stackStyle(Size(width = Fixed(10u), height = Fixed(10u))))

        // Fill children and SPACE_* spacers use `weight`, which would expand the Fit stack to the parent's maximum.
        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.START, fillChild, Orientation.Horizontal))
            .isTrue()
        assertThat(
            needsConstrainedFillLayout(stackSize, FlexDistribution.SPACE_BETWEEN, fixedChild, Orientation.Horizontal),
        ).isTrue()
        // Nothing weighted: Modifier.size alone handles the minimum.
        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.START, fixedChild, Orientation.Horizontal))
            .isFalse()
        // The minimum is on the width, so a vertical stack is unaffected.
        assertThat(needsConstrainedFillLayout(stackSize, FlexDistribution.SPACE_BETWEEN, fillChild, Orientation.Vertical))
            .isFalse()
    }

    private fun stackStyle(
        size: Size,
        overrides: List<PresentedOverride<PresentedStackPartial>> = emptyList(),
    ): StackComponentStyle = StackComponentStyle(
        children = emptyList(),
        dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = FlexDistribution.START),
        visible = true,
        size = size,
        spacing = 0.dp,
        background = null,
        padding = PaddingValues(0.dp),
        margin = PaddingValues(0.dp),
        shape = Shape.Rectangle(),
        border = null,
        shadow = null,
        badge = null,
        scrollOrientation = null,
        rcPackage = null,
        tabIndex = null,
        countdownDate = null,
        countFrom = CountdownComponent.CountFrom.DAYS,
        overrides = overrides,
    )

    @Test
    fun `fill minimum is reserved before distributing remaining space`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 100,
            constraints = listOf(Fill(min = 80u), Fill()),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(80, 20)
    }

    @Test
    fun `fill maximum releases space to unconstrained siblings`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 100,
            constraints = listOf(Fill(max = 20u), Fill()),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(20, 80)
    }

    @Test
    fun `rounding does not allocate beyond a fill maximum`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 41,
            constraints = listOf(Fill(max = 20u), Fill()),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(20, 21)
    }

    @Test
    fun `fill minimum is applied before sibling maximum`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 100,
            constraints = listOf(Fill(min = 100u), Fill(max = 40u)),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(100, 0)
    }

    @Test
    fun `space remaining after minimum is balanced between maximum constrained siblings`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 100,
            constraints = listOf(Fill(min = 80u), Fill(max = 20u), Fill(max = 20u)),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(80, 10, 10)
    }

    @Test
    fun `fill minimums are preserved when they exceed available space`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 100,
            constraints = listOf(Fill(min = 80u), Fill(min = 80u)),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(80, 80)
    }
}
