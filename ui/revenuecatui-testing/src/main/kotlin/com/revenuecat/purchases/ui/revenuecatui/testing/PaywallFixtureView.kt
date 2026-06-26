package com.revenuecat.purchases.ui.revenuecatui.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders a server-driven paywall from a fixture, deterministically and fully offline, for use in JVM
 * screenshot tests such as Paparazzi or Roborazzi.
 *
 * No [com.revenuecat.purchases.Purchases] instance, network access, or Google Play Billing is required.
 * Remote images are resolved from the fixture directory, and click handlers are no-ops (purchase flows
 * are out of scope for snapshot tests).
 *
 * Typical usage with Paparazzi:
 * ```kotlin
 * @get:Rule
 * val paywallFixturesRule = PaywallFixturesTestRule()
 *
 * private val fixtures = PaywallFixtures.load()
 *
 * @Test
 * fun defaultPaywall() {
 *     paparazzi.snapshot {
 *         PaywallFixtureView(fixtures.offering())
 *     }
 * }
 * ```
 *
 * Device configuration, theme, dark mode and locale remain under the control of the surrounding test
 * framework (e.g. Paparazzi's `deviceConfig`).
 */
@Composable
public fun PaywallFixtureView(
    fixtureOffering: PaywallFixtureOffering,
    modifier: Modifier = Modifier,
    options: PaywallFixtureViewOptions = PaywallFixtureViewOptions(),
) {
    ComponentsPaywallForTesting(
        offering = fixtureOffering.offering,
        imageResolver = fixtureOffering.imageResolver,
        storefrontCountryCode = options.storefrontCountryCode,
        date = options.date,
        renderValidationErrors =
        options.validationErrorStrategy == PaywallFixtureViewOptions.ValidationErrorStrategy.RENDER,
        modifier = modifier,
    )
}
