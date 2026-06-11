package com.revenuecat.purchases

import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingPaywallComponentsLazyTest {

    private fun offeringWith(components: Offering.PaywallComponents?) = Offering(
        identifier = "offering",
        serverDescription = "desc",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywall = null,
        paywallComponents = components,
        webCheckoutURL = null,
    )

    @Test
    fun `data is not decoded on construction and is decoded once on first access`() {
        var decodeCount = 0
        val components = Offering.PaywallComponents(uiConfig = mockk()) {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        assertThat(decodeCount).isEqualTo(0)

        components.data
        components.data

        // Decoded lazily on first access and cached thereafter.
        assertThat(decodeCount).isEqualTo(1)
    }

    @Test
    fun `hasPaywall is true without forcing the component tree decode`() {
        val components = Offering.PaywallComponents(uiConfig = mockk()) {
            error("paywall component tree must not be decoded just to answer hasPaywall")
        }
        val offering = offeringWith(components)

        // Must not throw — hasPaywall only checks for the presence of the wrapper, not its decoded contents.
        assertThat(offering.hasPaywall).isTrue()
    }
}
