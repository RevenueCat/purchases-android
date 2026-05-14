package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import com.revenuecat.purchases.Offering
import org.json.JSONObject

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
     * The raw paywall components config (what the dashboard returns under
     * `paywall_components.components_config`). The dashboard-aligned exporter walks this to
     * produce one entry per declared component, regardless of what Compose's unmerged tree
     * exposes.
     */
    val componentsConfigJson: JSONObject
}
