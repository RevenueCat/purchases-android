package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialCountdownComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PresentedCountdownPartialTests {

    @Test
    fun `combine keeps this visible when the other partial is null`() {
        val base = PresentedCountdownPartial(PartialCountdownComponent(visible = false))

        val combined = base.combine(with = null)

        assertThat(combined.partial.visible).isFalse()
    }

    @Test
    fun `combine takes the other partial's visible when present`() {
        val base = PresentedCountdownPartial(PartialCountdownComponent(visible = false))
        val other = PresentedCountdownPartial(PartialCountdownComponent(visible = true))

        val combined = base.combine(with = other)

        assertThat(combined.partial.visible).isTrue()
    }

    @Test
    fun `combine keeps this visible when the other partial's visible is null`() {
        val base = PresentedCountdownPartial(PartialCountdownComponent(visible = false))
        val other = PresentedCountdownPartial(PartialCountdownComponent(visible = null))

        val combined = base.combine(with = other)

        assertThat(combined.partial.visible).isFalse()
    }
}
