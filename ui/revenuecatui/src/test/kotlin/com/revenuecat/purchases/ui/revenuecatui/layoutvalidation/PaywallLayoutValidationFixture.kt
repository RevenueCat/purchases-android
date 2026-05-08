package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import com.revenuecat.purchases.Offering

/**
 * Describes everything `SemanticsLayoutExporterTest` needs to render and export a paywall.
 *
 * Implementations live alongside their fixture files (e.g. `MonikaWebTestData`). Adding a new
 * paywall to the layout-validation suite is just a matter of providing another implementation —
 * the test logic itself stays the same.
 */
internal interface PaywallLayoutValidationFixture {

    /** Stable identifier used in the offering and as the prefix for output JSON filenames. */
    val offeringId: String

    /** The fully-built offering containing the paywall components config and localizations. */
    val offering: Offering

    /**
     * `id -> name` lookup for every component declared in the fixture's components config.
     *
     * container's `name` (e.g. "Feature list", "Package list", "Button stack") as the node `label`.
     * Compose never sees those names — they live only in the
     * JSON config — so the exporter uses this map as a label fallback.
     */
    val componentNames: Map<String, String>

    /**
     * Component ids that should be replaced with the synthetic `paywall` prefix in the
     * normalized export, using `paywall_0`, `paywall_1`, … for top-level structural
     * buckets (the scroll container, sticky-footer wrappers, package-list
     * row, button-stack row, footer-buttons row).
     */
    val paywallSyntheticIds: Set<String>
}
