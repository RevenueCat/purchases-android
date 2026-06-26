package com.revenuecat.paywallstester

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.revenuecat.purchases.ui.revenuecatui.testing.PaywallFixtureView
import com.revenuecat.purchases.ui.revenuecatui.testing.PaywallFixtures
import com.revenuecat.purchases.ui.revenuecatui.testing.PaywallFixturesTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Offline Paparazzi snapshots of this app's dashboard paywalls via the purchases-ui-testing kit. To
 * refresh after dashboard changes, re-record fixtures then snapshots:
 *   REVENUECAT_API_KEY=<key> ./gradlew :examples:paywall-tester:recordPaywallFixtures
 *   ./gradlew :examples:paywall-tester:recordPaparazziBc8Debug --tests "*PaywallSnapshotTest"
 */
@RunWith(Parameterized::class)
class PaywallSnapshotTest(private val offeringId: String) {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @get:Rule
    val paywallFixturesRule = PaywallFixturesTestRule()

    @Test
    fun snapshot() {
        paparazzi.snapshot {
            PaywallFixtureView(fixtures.offering(offeringId))
        }
    }

    companion object {
        private val fixtures = PaywallFixtures.load()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun offerings(): List<String> = fixtures.offeringIds.filter { id ->
            runCatching { fixtures.offering(id) }.isSuccess
        }
    }
}
