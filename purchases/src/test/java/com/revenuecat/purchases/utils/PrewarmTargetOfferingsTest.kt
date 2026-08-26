package com.revenuecat.purchases.utils

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PrewarmTargetOfferingsTest {

    @Test
    fun `is just the current offering when there are no placements`() {
        val offerings = offeringsWith(current = offering("current"))

        assertThat(offerings.prewarmTargetOfferingIds()).containsExactly("current")
    }

    @Test
    fun `is empty when there is no current offering and no placements`() {
        assertThat(offeringsWith(current = null).prewarmTargetOfferingIds()).isEmpty()
    }

    @Test
    fun `includes the offering each placement resolves to`() {
        val offerings = offeringsWith(
            current = offering("current"),
            others = listOf(offering("onboarding"), offering("settings")),
            placements = Offerings.Placements(
                fallbackOfferingId = null,
                offeringIdsByPlacement = mapOf("onboarding" to "onboarding", "settings" to "settings"),
            ),
        )

        assertThat(offerings.prewarmTargetOfferingIds())
            .containsExactlyInAnyOrder("current", "onboarding", "settings")
    }

    @Test
    fun `includes the placement fallback offering`() {
        val offerings = offeringsWith(
            current = null,
            others = listOf(offering("fallback")),
            placements = Offerings.Placements(
                fallbackOfferingId = "fallback",
                offeringIdsByPlacement = emptyMap(),
            ),
        )

        assertThat(offerings.prewarmTargetOfferingIds()).containsExactly("fallback")
    }

    // A placement mapped to null means "show nothing here".
    @Test
    fun `skips a placement mapped to no offering`() {
        val offerings = offeringsWith(
            current = null,
            placements = Offerings.Placements(
                fallbackOfferingId = null,
                offeringIdsByPlacement = mapOf("onboarding" to null),
            ),
        )

        assertThat(offerings.prewarmTargetOfferingIds()).isEmpty()
    }

    @Test
    fun `reports an id once when several placements resolve to the same offering`() {
        val offerings = offeringsWith(
            current = offering("current"),
            others = listOf(offering("shared")),
            placements = Offerings.Placements(
                fallbackOfferingId = "shared",
                offeringIdsByPlacement = mapOf("a" to "shared", "b" to "shared"),
            ),
        )

        assertThat(offerings.prewarmTargetOfferingIds()).containsExactlyInAnyOrder("current", "shared")
    }

    @Test
    fun `reports an id a placement names even when the response has no such offering`() {
        val offerings = offeringsWith(
            current = null,
            placements = Offerings.Placements(
                fallbackOfferingId = null,
                offeringIdsByPlacement = mapOf("onboarding" to "missing"),
            ),
        )

        assertThat(offerings.prewarmTargetOfferingIds()).containsExactly("missing")
        assertThat(offerings.prewarmTargetOfferings()).isEmpty()
    }

    @Test
    fun `orders placements alphabetically between the current offering and the fallback`() {
        val offerings = offeringsWith(
            current = offering("current"),
            others = listOf(offering("zebra"), offering("alpha"), offering("fallback")),
            placements = Offerings.Placements(
                fallbackOfferingId = "fallback",
                offeringIdsByPlacement = linkedMapOf("zebra" to "zebra", "alpha" to "alpha"),
            ),
        )

        assertThat(offerings.prewarmTargetOfferingIds())
            .containsExactly("current", "alpha", "zebra", "fallback")
    }

    @Test
    fun `resolves ids to offerings in the same order`() {
        val current = offering("current")
        val onboarding = offering("onboarding")
        val offerings = offeringsWith(
            current = current,
            others = listOf(onboarding),
            placements = Offerings.Placements(
                fallbackOfferingId = null,
                offeringIdsByPlacement = mapOf("onboarding" to "onboarding"),
            ),
        )

        assertThat(offerings.prewarmTargetOfferings()).containsExactly(current, onboarding)
    }

    private fun offering(identifier: String): Offering = mockk<Offering>().apply {
        every { this@apply.identifier } returns identifier
    }

}
