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
        val components = Offering.PaywallComponents(uiConfig = mockk(), componentsHash = "hash") {
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
        val components = Offering.PaywallComponents(uiConfig = mockk(), componentsHash = "hash") {
            error("paywall component tree must not be decoded just to answer hasPaywall")
        }
        val offering = offeringWith(components)

        // Must not throw — hasPaywall only checks for the presence of the wrapper, not its decoded contents.
        assertThat(offering.hasPaywall).isTrue()
    }

    @Test
    fun `hasPaywall is true when hasPaywallComponents flag is set even though paywallComponents is skipped`() {
        // Simulates the workflows path: components are not captured (null) but presence is flagged.
        val offering = Offering(
            identifier = "offering",
            serverDescription = "desc",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywall = null,
            paywallComponents = null,
            webCheckoutURL = null,
        ).also { it.hasPaywallComponents = true }

        assertThat(offering.hasPaywall).isTrue()
    }

    @Test
    fun `equals is true for same uiConfig and componentsHash, without forcing the decode`() {
        val uiConfig = mockk<UiConfig>()
        var decodeCount = 0
        val first = Offering.PaywallComponents(uiConfig = uiConfig, componentsHash = "hash") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }
        val second = Offering.PaywallComponents(uiConfig = uiConfig, componentsHash = "hash") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        assertThat(first == second).isTrue()
        // Equality compares the cheap content hash, never the lazily-decoded data.
        assertThat(decodeCount).isEqualTo(0)
    }

    @Test
    fun `equals is false when componentsHash differs, without forcing the decode`() {
        val uiConfig = mockk<UiConfig>()
        var decodeCount = 0
        val first = Offering.PaywallComponents(uiConfig = uiConfig, componentsHash = "hash-a") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }
        val second = Offering.PaywallComponents(uiConfig = uiConfig, componentsHash = "hash-b") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        assertThat(first == second).isFalse()
        assertThat(decodeCount).isEqualTo(0)
    }

    @Test
    fun `equals is reflexive without forcing the decode`() {
        var decodeCount = 0
        val components = Offering.PaywallComponents(uiConfig = mockk(), componentsHash = "hash") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        // `this === other` short-circuits before touching the lazy data.
        assertThat(components.equals(components)).isTrue()
        assertThat(decodeCount).isEqualTo(0)
    }

    @Test
    fun `hashCode is equal for instances with the same uiConfig and componentsHash, without forcing the decode`() {
        val uiConfig = mockk<UiConfig>()
        var decodeCount = 0
        val first = Offering.PaywallComponents(uiConfig = uiConfig, componentsHash = "hash") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }
        val second = Offering.PaywallComponents(uiConfig = uiConfig, componentsHash = "hash") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        assertThat(first.hashCode()).isEqualTo(second.hashCode())
        assertThat(decodeCount).isEqualTo(0)
    }

    @Test
    fun `toString does not include or force the decode of the component tree`() {
        var decodeCount = 0
        val components = Offering.PaywallComponents(uiConfig = mockk(), componentsHash = "hash") {
            decodeCount++
            mockk<PaywallComponentsData>()
        }

        assertThat(components.toString()).contains("PaywallComponents(")
        assertThat(decodeCount).isEqualTo(0)
    }

    @Test
    fun `equals compares decoded data for instances built from already-decoded data`() {
        val uiConfig = mockk<UiConfig>()
        val data = mockk<PaywallComponentsData>()
        val first = Offering.PaywallComponents(uiConfig = uiConfig, data = data)
        val second = Offering.PaywallComponents(uiConfig = uiConfig, data = data)

        // The data-based constructor derives its key from the already-decoded data's structural hash, so
        // comparing it is free (nothing to decode).
        assertThat(first == second).isTrue()
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `equals is false when the decoded data differs`() {
        val uiConfig = mockk<UiConfig>()
        val first = Offering.PaywallComponents(uiConfig = uiConfig, data = mockk())
        val second = Offering.PaywallComponents(uiConfig = uiConfig, data = mockk())

        assertThat(first == second).isFalse()
    }
}
