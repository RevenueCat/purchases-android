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

    @Test
    fun `equals is true for same uiConfig and data, and forces the decode of both`() {
        val uiConfig = mockk<com.revenuecat.purchases.UiConfig>()
        val data = mockk<PaywallComponentsData>()
        var decodeCount = 0
        val first = Offering.PaywallComponents(uiConfig = uiConfig) { decodeCount++; data }
        val second = Offering.PaywallComponents(uiConfig = uiConfig) { decodeCount++; data }

        assertThat(first == second).isTrue()
        // Equality compares the decoded `data`, so accessing it forces the decode of both sides.
        assertThat(decodeCount).isEqualTo(2)
    }

    @Test
    fun `equals is false when the decoded data differs`() {
        val uiConfig = mockk<com.revenuecat.purchases.UiConfig>()
        val first = Offering.PaywallComponents(uiConfig = uiConfig, data = mockk())
        val second = Offering.PaywallComponents(uiConfig = uiConfig, data = mockk())

        assertThat(first == second).isFalse()
    }

    @Test
    fun `equals is reflexive without forcing the decode`() {
        var decodeCount = 0
        val components = Offering.PaywallComponents(uiConfig = mockk()) {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        // `this === other` short-circuits before touching the lazy `data`.
        assertThat(components.equals(components)).isTrue()
        assertThat(decodeCount).isEqualTo(0)
    }

    @Test
    fun `hashCode is equal for equal instances`() {
        val uiConfig = mockk<com.revenuecat.purchases.UiConfig>()
        val data = mockk<PaywallComponentsData>()
        val first = Offering.PaywallComponents(uiConfig = uiConfig, data = data)
        val second = Offering.PaywallComponents(uiConfig = uiConfig, data = data)

        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `toString includes the decoded data`() {
        val data = mockk<PaywallComponentsData>()
        val components = Offering.PaywallComponents(uiConfig = mockk(), data = data)

        assertThat(components.toString())
            .contains("PaywallComponents(")
            .contains(data.toString())
    }
}
