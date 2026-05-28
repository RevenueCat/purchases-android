package com.revenuecat.purchases.rules

/**
 * Errors surfaced by the rules engine.
 *
 * Note on missing variables: the v1 evaluator does **not** raise an error
 * for them — per the JSON Logic spec, they resolve to `null` and a warning
 * is logged instead. If a strict mode is ever needed, we'd add a
 * `MissingVariable` subclass.
 */
internal sealed class RuleError(message: String) : RuntimeException(message) {

    /** The predicate JSON could not be parsed. */
    data class Parse(val reason: String) : RuleError("failed to parse predicate JSON: $reason")

    /**
     * The predicate references a JSON Logic operator the engine does not
     * implement. Carries the operator name so callers can decide policy
     * (default-deny, log, etc.).
     */
    data class UnsupportedOperator(val name: String) : RuleError("unsupported operator: $name")
}
