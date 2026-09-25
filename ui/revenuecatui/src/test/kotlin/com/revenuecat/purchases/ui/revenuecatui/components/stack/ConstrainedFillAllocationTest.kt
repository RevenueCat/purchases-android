package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConstrainedFillAllocationTest {

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
