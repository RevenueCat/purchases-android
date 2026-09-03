package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
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
    fun `fill minimums are preserved when they exceed available space`() {
        val allocations = allocateConstrainedFillSpace(
            availableSpace = 100,
            constraints = listOf(Fill(min = 80u), Fill(min = 80u)),
            density = Density(1f),
        )

        assertThat(allocations).containsExactly(80, 80)
    }
}
