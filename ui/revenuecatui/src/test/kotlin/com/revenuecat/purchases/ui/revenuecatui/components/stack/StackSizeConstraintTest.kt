package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
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
    fun `fit without a positive minimum keeps the existing layout path`() {
        assertThat(Fit().requiresFitMinimumLayout(FlexDistribution.SPACE_BETWEEN)).isFalse()
        assertThat(Fit(max = 100u).requiresFitMinimumLayout(FlexDistribution.SPACE_AROUND)).isFalse()
        assertThat(Fit(min = 0u).requiresFitMinimumLayout(FlexDistribution.SPACE_EVENLY)).isFalse()
    }

    @Test
    fun `fit with a positive minimum requires custom layout only for flexible distributions`() {
        val fit = Fit(min = 100u)

        assertThat(fit.requiresFitMinimumLayout(FlexDistribution.SPACE_BETWEEN)).isTrue()
        assertThat(fit.requiresFitMinimumLayout(FlexDistribution.SPACE_AROUND)).isTrue()
        assertThat(fit.requiresFitMinimumLayout(FlexDistribution.SPACE_EVENLY)).isTrue()
        assertThat(fit.requiresFitMinimumLayout(FlexDistribution.START)).isFalse()
        assertThat(fit.requiresFitMinimumLayout(FlexDistribution.CENTER)).isFalse()
        assertThat(fit.requiresFitMinimumLayout(FlexDistribution.END)).isFalse()
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
