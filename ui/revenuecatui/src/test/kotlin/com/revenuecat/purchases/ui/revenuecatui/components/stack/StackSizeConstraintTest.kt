package com.revenuecat.purchases.ui.revenuecatui.components.stack

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
}
