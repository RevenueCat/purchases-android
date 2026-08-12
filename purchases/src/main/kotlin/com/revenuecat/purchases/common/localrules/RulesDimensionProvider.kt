package com.revenuecat.purchases.common.localrules

import java.util.Date

/**
 * The SDK-owned roots a predicate can read. Closed on purpose, like `RemoteConfigTopic`: these names are part of
 * the contract predicates are authored against, so a provider must not be able to invent one.
 *
 * Each entry becomes a real nested object in the evaluated scope, because the engine's `var` operator walks
 * nested objects by strict dot-path and has no flat-key fallback — `device.app_version` resolves *through*
 * `device`, it is never a literal key.
 */
internal enum class RulesDimensionNamespace(val key: String) {
    Device("device"),
}

/**
 * A scalar exposed to the rules engine.
 *
 * Keeps providers independent from the engine's own value representation, and encodes that a dimension is a
 * scalar: arrays and objects are deliberately not expressible yet.
 */
internal sealed class RulesDimensionValue {
    internal data class StringValue(val value: String) : RulesDimensionValue()
    internal data class BoolValue(val value: Boolean) : RulesDimensionValue()
    internal data class IntValue(val value: Long) : RulesDimensionValue()
    internal data class DoubleValue(val value: Double) : RulesDimensionValue()
}

/**
 * Supplies one subtree of on-device dimensions.
 *
 * Implementations may observe or persist state internally, but values are pulled only when an evaluation asks for
 * a new snapshot, since eligibility reflects the customer's state at the moment the rule is evaluated.
 */
internal interface RulesDimensionProvider {

    /** Stable identifier, used only for diagnostics. */
    val identifier: String

    /** The root this provider's values are nested under. */
    val namespace: RulesDimensionNamespace

    /**
     * The complete current set of values relative to [namespace], keyed in lowercase snake_case (`app_version`);
     * [RulesDimensionResolver] adds the namespace.
     *
     * A value that is unavailable is omitted rather than guessed: an absent key resolves to null in the engine,
     * which is a non-match rather than an error. Throwing is reserved for a systemic failure to produce this
     * provider's values.
     *
     * [date] is the common reference instant for the evaluation. It does not indicate when the underlying values
     * were observed.
     */
    suspend fun dimensions(date: Date): Map<String, RulesDimensionValue>
}
