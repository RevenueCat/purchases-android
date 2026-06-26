package com.revenuecat.purchases.ui.revenuecatui.testing

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

/**
 * Self-test of the testing kit: renders fixture paywalls under Paparazzi exactly like an SDK consumer
 * would.
 */
class PaywallFixtureViewTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @get:Rule
    val paywallFixturesRule = PaywallFixturesTestRule()

    private val fixtures = PaywallFixtures.load()

    @Test
    fun rendersCurrentOffering() {
        paparazzi.snapshot {
            PaywallFixtureView(fixtures.offering())
        }
    }

    @Test
    fun rendersWithProductOverrides() {
        val annualOverride = TestStoreProduct(
            id = "cheapest_subs",
            name = "Annual",
            title = "Annual (Snapshot test)",
            price = Price(amountMicros = 129_990_000, currencyCode = "KRW", formatted = "₩129,990"),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )
        paparazzi.snapshot {
            PaywallFixtureView(fixtures.offering(products = listOf(annualOverride)))
        }
    }

    @Test
    fun exposesOfferingIds() {
        assertThat(fixtures.offeringIds).containsExactly("default")
    }

    @Test
    fun failsWithDescriptiveErrorForUnknownOffering() {
        assertThatThrownBy { fixtures.offering(offeringId = "nonexistent") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("nonexistent")
            .hasMessageContaining("default")
    }

    @Test
    fun failsWithDescriptiveErrorForMissingFixture() {
        assertThatThrownBy { PaywallFixtures.load(resourcesRoot = "missing-fixtures") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("/missing-fixtures/offerings.json")
            .hasMessageContaining("recordPaywallFixtures")
    }
}
