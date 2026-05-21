package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value

/**
 * Comparison operators: `<`, `<=`, `>`, `>=`.
 *
 * Mirrors the JSON Logic / ECMAScript Abstract Relational Comparison
 * rules:
 *
 * - **Both operands are strings** → lexicographic comparison
 *   (`"10" < "9"` is `true` because `'1' < '9'` byte-wise).
 * - **Otherwise** → coerce both operands to [Double] via
 *   [Value.toNumberOrNull] and compare numerically. Operands that
 *   can't coerce ([Value.ObjectValue], [Value.ArrayValue], unparseable
 *   strings) become [Double.NaN]; per IEEE 754 every comparison
 *   against NaN returns `false`, so a malformed operand makes the
 *   predicate fail closed — a safe default for rule authoring.
 *
 * `<` and `<=` accept a 3-arg "between" form per the JSON Logic spec:
 * `{"<": [a, b, c]}` reads as `a < b AND b < c`. `>` and `>=` are
 * binary only, matching the JS reference implementation.
 */
internal object ComparisonOperators {

    /** `{"<": [a, b]}` — `a < b`. `{"<": [a, b, c]}` — `a < b AND b < c`. */
    fun opLt(args: Value, vars: Value): Value =
        evalChain(args, vars, "<", Comparator.LESS)

    /** `{"<=": [a, b]}` — `a <= b`. `{"<=": [a, b, c]}` — `a <= b AND b <= c`. */
    fun opLe(args: Value, vars: Value): Value =
        evalChain(args, vars, "<=", Comparator.LESS_OR_EQUAL)

    /** `{">": [a, b]}` — `a > b`. Strictly binary; matches the JS reference. */
    fun opGt(args: Value, vars: Value): Value =
        evalBinary(args, vars, ">", Comparator.GREATER)

    /** `{">=": [a, b]}` — `a >= b`. Strictly binary; matches the JS reference. */
    fun opGe(args: Value, vars: Value): Value =
        evalBinary(args, vars, ">=", Comparator.GREATER_OR_EQUAL)

    private const val BINARY_ARITY = 2
    private const val BETWEEN_ARITY = 3

    /**
     * Comparator dispatch. Driven from one place so the [String] (lex)
     * and [Double] (numeric) paths in [compare] stay in lockstep.
     *
     * Kept as two explicit overloads (rather than one generic
     * `Comparable<T>`-bounded function) so each path resolves to the
     * primitive operator on its operand type. That matters for [Double]:
     * Kotlin's `<` on `Double` primitives is IEEE 754 — every
     * comparison involving `NaN` returns `false` — whereas going
     * through `Comparable<Double>.compareTo` would use total order
     * (which sorts `NaN` greater than `+Infinity`) and silently flip
     * the NaN-fail-closed semantics the doc above promises.
     */
    private enum class Comparator {
        LESS,
        LESS_OR_EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        ;

        fun apply(lhs: Double, rhs: Double): Boolean = when (this) {
            LESS -> lhs < rhs
            LESS_OR_EQUAL -> lhs <= rhs
            GREATER -> lhs > rhs
            GREATER_OR_EQUAL -> lhs >= rhs
        }

        fun apply(lhs: String, rhs: String): Boolean = when (this) {
            LESS -> lhs < rhs
            LESS_OR_EQUAL -> lhs <= rhs
            GREATER -> lhs > rhs
            GREATER_OR_EQUAL -> lhs >= rhs
        }
    }

    /**
     * Two-string operands → lex. Otherwise → numeric coercion. Encodes
     * the JSON Logic / JS spec's "lex only when BOTH operands are
     * strings" branch of Abstract Relational Comparison.
     */
    private fun compare(lhs: Value, rhs: Value, cmp: Comparator): Boolean {
        if (lhs is Value.StringValue && rhs is Value.StringValue) {
            return cmp.apply(lhs.value, rhs.value)
        }
        return cmp.apply(lhs.asDouble(), rhs.asDouble())
    }

    /**
     * Shared 2-or-3 arg "chain" evaluator used by `<` and `<=`. The
     * 3-arg form is the JSON Logic between-form: each adjacent pair
     * must satisfy [cmp].
     */
    private fun evalChain(
        args: Value,
        vars: Value,
        opName: String,
        cmp: Comparator,
    ): Value {
        val evaluated = Operators.evalArgs(args, vars)
        return when (evaluated.size) {
            BINARY_ARITY -> Value.BoolValue(compare(evaluated[0], evaluated[1], cmp))
            BETWEEN_ARITY -> Value.BoolValue(
                compare(evaluated[0], evaluated[1], cmp) &&
                    compare(evaluated[1], evaluated[2], cmp),
            )
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
        opName: String,
        cmp: Comparator,
    ): Value {
        val evaluated = Operators.evalArgs(args, vars)
        if (evaluated.size != BINARY_ARITY) {
            throw RuleError.TypeMismatch(
                "operator '$opName' expects 2 arguments, got ${evaluated.size}",
            )
        }
        return Value.BoolValue(compare(evaluated[0], evaluated[1], cmp))
    }

    /**
     * Coerce to [Double], falling back to [Double.NaN] for non-numeric
     * operands so comparisons against malformed inputs return `false`
     * per IEEE 754.
     */
    private fun Value.asDouble(): Double = toNumberOrNull() ?: Double.NaN
}
