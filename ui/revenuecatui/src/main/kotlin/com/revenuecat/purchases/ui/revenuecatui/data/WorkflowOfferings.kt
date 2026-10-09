package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

/**
 * The offerings a workflow presentation resolves against. Wraps the fetched [offerings] together with the
 * developer-supplied [Offering] from `PaywallOptions.offeringSelection`, if any, so every lookup (steps, exit
 * offers) goes through one rule: the developer-provided instance wins for its own identifier, otherwise the
 * fetched entry is used. That keeps developer modifications (e.g. filtered packages or extra metadata) in
 * what the workflow renders, without each call site having to remember the override.
 */
internal data class WorkflowOfferings(
    val offerings: Offerings,
    val developerProvidedOffering: Offering?,
) {
    operator fun get(identifier: String): Offering? =
        developerProvidedOffering?.takeIf { it.identifier == identifier } ?: offerings[identifier]
}
