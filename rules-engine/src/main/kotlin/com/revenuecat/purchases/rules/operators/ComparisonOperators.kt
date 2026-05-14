package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.RulesEngineLogger
import com.revenuecat.purchases.rules.Value

/**
 * Comparison operators: `<`, `<=`, `>`, `>=`.
 *
 * All operators coerce operands through [Value.toNumberOrNull] and
 * compare as `Double`. Operands that don't coerce ([Value.ObjectValue],
 * [Value.ArrayValue], unparseable strings) become [Double.NaN]; per
 * IEEE 754 every comparison against NaN returns `false`, so a
 * malformed operand makes the predicate fail closed — a safe default
 * for rule authoring.
 *
 * `<` and `<=` accept a 3-arg "between" form per the JSON Logic spec:
 * `{"<": [a, b, c]}` reads as `a < b AND b < c`. `>` and `>=` are binary
 * only, matching the JS reference implementation.
 *
 * Note on string semantics: the JS reference compares two strings
 * lexicographically (`"10" < "9"` is true) and only coerces when types
 * mix. We always coerce numerically, which gives the more intuitive
 * `"10" < "9"` is false.
 */
internal object ComparisonOperators {

    /** `{"<": [a, b]}` — `a < b`. `{"<": [a, b, c]}` — `a < b AND b < c`. */
    fun opLt(args: Value, vars: Value, logger: RulesEngineLogger): Value =
        evalChain(args, vars, logger, "<") { x, y -> x < y }

    /** `{"<=": [a, b]}` — `a <= b`. `{"<=": [a, b, c]}` — `a <= b AND b <= c`. */
    fun opLe(args: Value, vars: Value, logger: RulesEngineLogger): Value =
        evalChain(args, vars, logger, "<=") { x, y -> x <= y }

    /** `{">": [a, b]}` — `a > b`. Strictly binary; matches the JS reference. */
    fun opGt(args: Value, vars: Value, logger: RulesEngineLogger): Value =
        evalBinary(args, vars, logger, ">") { x, y -> x > y }

    /** `{">=": [a, b]}` — `a >= b`. Strictly binary; matches the JS reference. */
    fun opGe(args: Value, vars: Value, logger: RulesEngineLogger): Value =
        evalBinary(args, vars, logger, ">=") { x, y -> x >= y }

    private const val BINARY_ARITY = 2
    private const val BETWEEN_ARITY = 3

    /**
     * Shared 2-or-3 arg "chain" evaluator used by `<` and `<=`. The
     * 3-arg form is the JSON Logic between-form: each adjacent pair
     * must satisfy [cmp].
     */
    private fun evalChain(
        args: Value,
        vars: Value,
        logger: RulesEngineLogger,
        opName: String,
        cmp: (Double, Double) -> Boolean,
    ): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        return when (evaluated.size) {
            BINARY_ARITY -> Value.BoolValue(cmp(evaluated[0].asDouble(), evaluated[1].asDouble()))
            BETWEEN_ARITY -> {
                val left = evaluated[0].asDouble()
                val mid = evaluated[1].asDouble()
                val right = evaluated[2].asDouble()
                Value.BoolValue(cmp(left, mid) && cmp(mid, right))
            }
            else -> throw RuleError.TypeMismatch(
                "operator '$opName' expects 2 or 3 arguments, got ${evaluated.size}",
            )
        }
    }

    /**
     * Shared 2-arg evaluator used by `>` and `>=`. No between-form per
     * the JSON Logic spec / JS reference.
     */
    private fun evalBinary(
        args: Value,
        vars: Value,
        logger: RulesEngineLogger,
        opName: String,
        cmp: (Double, Double) -> Boolean,
    ): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        if (evaluated.size != BINARY_ARITY) {
            throw RuleError.TypeMismatch(
                "operator '$opName' expects 2 arguments, got ${evaluated.size}",
            )
        }
        return Value.BoolValue(cmp(evaluated[0].asDouble(), evaluated[1].asDouble()))
    }

    /**
     * Coerce to [Double], falling back to [Double.NaN] for non-numeric
     * operands so comparisons against malformed inputs return `false`
     * per IEEE 754.
     */
    private fun Value.asDouble(): Double = toNumberOrNull() ?: Double.NaN
}
