package com.revenuecat.purchases.ui.revenuecatui.testing

import java.util.Date

/**
 * Options controlling how [PaywallFixtureView] renders a fixture paywall. All defaults are chosen for
 * deterministic snapshots.
 */
public class PaywallFixtureViewOptions @JvmOverloads public constructor(
    /**
     * The storefront country code used for price formatting decisions (e.g. whether to show decimals).
     */
    public val storefrontCountryCode: String? = "US",
    /**
     * The fixed "current time" used for date-dependent rendering (e.g. countdown components). Defaults
     * to a constant date so that snapshots are reproducible across test runs.
     */
    public val date: Date = Date(DEFAULT_FIXTURE_DATE_MILLIS),
    /**
     * What to do when the paywall fails validation (e.g. the fixture was recorded with paywall features
     * this SDK version cannot parse).
     */
    public val validationErrorStrategy: ValidationErrorStrategy = ValidationErrorStrategy.THROW,
) {

    /** Strategy for handling paywall validation errors. */
    public enum class ValidationErrorStrategy {
        /** Throw an exception, failing the test with the validation error messages. */
        THROW,

        /** Render the validation error messages as text, making them visible in the snapshot. */
        RENDER,
    }

    public companion object {
        /** 2025-04-23 UTC. The same fixed date is used by RevenueCat's internal paywall previews. */
        public const val DEFAULT_FIXTURE_DATE_MILLIS: Long = 1745366400000
    }
}
