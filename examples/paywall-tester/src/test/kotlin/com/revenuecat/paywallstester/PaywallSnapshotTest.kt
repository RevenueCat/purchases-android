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
 * Snapshot tests for the paywalls of the RevenueCat dashboard project this app points at, rendered
 * fully offline with the `purchases-ui-testing` kit — no `Purchases.configure()`, network, or Google
 * Play Billing required.
 *
 * Fixtures (offerings JSON + CDN images) live in src/test/resources/revenuecat-paywall-fixtures and are
 * regenerated with the recorder plugin whenever the dashboard changes:
 *
 *     REVENUECAT_API_KEY=<public sdk key> ./gradlew :examples:paywall-tester:recordPaywallFixtures
 *
 * Then re-record the snapshots to review the visual diff:
 *
 *     ./gradlew :examples:paywall-tester:recordPaparazziBc8Debug --tests "*PaywallSnapshotTest"
 *
 * The parameter list is pre-filtered to offerings that parse into a components paywall with packages, so
 * empty/legacy offerings are skipped cleanly rather than failing the run.
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
