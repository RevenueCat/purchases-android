package com.revenuecat.purchases.ui.revenuecatui.testing

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit rule required when rendering paywalls with [PaywallFixtureView] in non-Robolectric JVM tests
 * (e.g. Paparazzi). Paywall rendering launches coroutines on the main dispatcher, which does not exist
 * in a plain JVM test; this rule installs one for the duration of each test. Without it, rendering
 * deadlocks.
 *
 * ```kotlin
 * @get:Rule
 * val paywallFixturesRule = PaywallFixturesTestRule()
 * ```
 */
public class PaywallFixturesTestRule : TestRule {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val mainDispatcher = newSingleThreadContext("PaywallFixtures-main-dispatcher")
            Dispatchers.setMain(mainDispatcher)
            try {
                base.evaluate()
            } finally {
                Dispatchers.resetMain()
                mainDispatcher.close()
            }
        }
    }
}
