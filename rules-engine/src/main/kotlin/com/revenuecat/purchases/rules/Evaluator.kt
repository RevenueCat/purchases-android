package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.operators.Operators

/**
 * Top-level evaluator for JSON Logic predicates.
 *
 * The evaluator is intentionally simple: literals evaluate to themselves,
 * arrays evaluate element-wise, single-key objects dispatch to an operator,
 * multi-key objects are treated as literal data. Operators handle their
 * own short-circuit / arity logic.
 *
 * Diagnostic warnings flow through [RulesEngineLog] so the engine
 * internals don't have to thread a logger argument through every call.
 */
internal object Evaluator {

    /**
     * Module-internal entry point. A future iteration will surface this via
     * the SDK-facing API.
     *
     * @param predicate The inner `predicate` field of a rule artifact,
     *  already parsed into a typed [Value] tree by the caller (the engine
     *  never sees the JSON wire format — see module-level docs in
     *  `Value.kt` for why).
     * @param variables The resolved variable map — typically a nested
     *  object mirroring the namespace hierarchy (`subscriber.*`,
     *  `session.*`, etc.).
     * @return `true` when the predicate evaluates to a truthy value per
     *  JSON Logic rules.
     */
    fun evaluate(
        predicate: Value,
        variables: Map<String, Value>,
    ): Boolean {
        val scope = Value.ObjectValue(variables)
        return evaluateValue(predicate, scope).isTruthy
    }

    /**
     * Recursive evaluator. Module-internal so operator implementations can
     * call it for short-circuit / nested evaluation.
     */
    fun evaluateValue(
        predicate: Value,
        vars: Value,
    ): Value = when (predicate) {
        Value.Null,
        is Value.BoolValue,
        is Value.IntValue,
        is Value.FloatValue,
        is Value.StringValue,
        -> predicate

        is Value.ArrayValue -> Value.ArrayValue(
            predicate.items.map { evaluateValue(it, vars) },
        )

        is Value.ObjectValue -> {
            val entries = predicate.entries
            if (entries.size == 1) {
                val (operatorName, args) = entries.entries.first()
                Operators.dispatch(operatorName, args, vars)
            } else {
                predicate
            }
        }
    }
}
