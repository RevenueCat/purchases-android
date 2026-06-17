package com.revenuecat.paywallfixturessample

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.testing.PaywallFixtureView
import com.revenuecat.purchases.ui.revenuecatui.testing.PaywallFixtures
import com.revenuecat.purchases.ui.revenuecatui.testing.PaywallFixturesTestRule
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

/**
 * Snapshot tests of this app's paywalls, written exactly like an SDK consumer would write them.
 *
 * The fixtures under src/test/resources/revenuecat-paywall-fixtures were recorded with the
 * recordPaywallFixtures Gradle task. Whenever the paywall design changes on the RevenueCat dashboard,
 * re-record the fixtures and re-record the Paparazzi snapshots to review the visual diff.
 */
class PaywallSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @get:Rule
    val paywallFixturesRule = PaywallFixturesTestRule()

    private val fixtures = PaywallFixtures.load()

    @Test
    fun currentOffering() {
        paparazzi.snapshot {
            PaywallFixtureView(fixtures.offering())
        }
    }

    @Test
    fun currentOffering_withLocalizedPrices() {
        val annual = TestStoreProduct(
            id = "cheapest_subs",
            name = "Annual",
            title = "Annual",
            price = Price(amountMicros = 129_990_000, currencyCode = "KRW", formatted = "₩129,990"),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )
        paparazzi.snapshot {
            PaywallFixtureView(fixtures.offering(products = listOf(annual)))
        }
    }

    @Test
    fun unparseablePaywallFailsWithDescriptiveError() {
        val brokenFixtures = PaywallFixtures.load(resourcesRoot = "broken-fixtures")
        assertThatThrownBy { brokenFixtures.offering() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("has no components paywall")
            .hasMessageContaining("re-recording the fixture or upgrading the SDK")
    }
}

/**
 * Dark mode snapshot, using Paparazzi's own device configuration — the kit leaves device, theme, locale
 * and dark mode under the test framework's control.
 */
class PaywallDarkModeSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT))

    @get:Rule
    val paywallFixturesRule = PaywallFixturesTestRule()

    private val fixtures = PaywallFixtures.load()

    @Test
    fun currentOffering_darkMode() {
        paparazzi.snapshot {
            PaywallFixtureView(fixtures.offering())
        }
    }
}
