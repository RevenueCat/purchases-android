@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

/**
 * The SDK-owned roots a predicate can read. Closed on purpose, like `RemoteConfigTopic`: these names are part of
 * the contract predicates are authored against, so a provider must not be able to invent one.
 *
 * Each entry becomes a real nested object in the evaluated scope, because the engine's `var` operator walks
 * nested objects by strict dot-path and has no flat-key fallback — `device.appVersion` resolves *through*
 * `device`, it is never a literal key.
 */
internal enum class RulesDimensionNamespace(val key: String) {
    /** Predicate results pre-evaluated by the backend, supplied per evaluation; see [LocalRulesEvaluator.match]. */
    Backend("backend"),

    /** Values supplied by the caller for one evaluation; see [LocalRulesEvaluator.match]. */
    Custom("custom"),
    Device("device"),
    Store("store"),

    /** What the app has told the SDK about the customer; see `Purchases.setAttributes`. */
    SubscriberAttributes("subscriberAttributes"),
}

/**
 * Supplies one subtree of on-device dimensions.
 *
 * Implementations may observe or persist state internally, but values are pulled only when an evaluation asks for
 * a new snapshot, since eligibility reflects the customer's state at the moment the rule is evaluated.
 */
internal interface RulesDimensionProvider {

    /** The root this provider's values are nested under, and the name it is reported under in diagnostics. */
    val namespace: RulesDimensionNamespace

    /**
     * The complete current set of values relative to [namespace], keyed in camelCase (`appVersion`);
     * [RulesDimensionResolver] adds the namespace.
     *
     * A value that is unavailable is omitted rather than guessed: a predicate that reads an absent key fails as
     * an unresolved variable, rather than being answered from a value we invented. A name no predicate could
     * read is dropped by [RulesDimensionResolver], which applies that rule to every provider. Throwing is
     * reserved for a systemic failure to produce this provider's values.
     *
     * [date] is the common reference instant for the evaluation. It does not indicate when the underlying values
     * were observed.
     */
    suspend fun dimensions(date: Date): Map<String, RulesDimensionValue>
}
