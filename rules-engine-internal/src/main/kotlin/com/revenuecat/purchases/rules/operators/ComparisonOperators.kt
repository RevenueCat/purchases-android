package com.revenuecat.purchases.rules.operators

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
        evalChain(args, vars, Comparator.LESS)

    /** `{"<=": [a, b]}` — `a <= b`. `{"<=": [a, b, c]}` — `a <= b AND b <= c`. */
    fun opLe(args: Value, vars: Value): Value =
        evalChain(args, vars, Comparator.LESS_OR_EQUAL)

    /** `{">": [a, b]}` — `a > b`. Strictly binary; matches the JS reference. */
    fun opGt(args: Value, vars: Value): Value =
        evalBinary(args, vars, Comparator.GREATER)

    /** `{">=": [a, b]}` — `a >= b`. Strictly binary; matches the JS reference. */
    fun opGe(args: Value, vars: Value): Value =
        evalBinary(args, vars, Comparator.GREATER_OR_EQUAL)

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
     *
     * `null` lhs/rhs means that argument was omitted (e.g. `{">": [1]}`);
     * we coerce it to [Double.NaN], and any comparison involving NaN
     * is `false`.
     */
    private fun compare(lhs: Value?, rhs: Value?, cmp: Comparator): Boolean {
        if (lhs is Value.StringValue && rhs is Value.StringValue) {
            return cmp.apply(lhs.value, rhs.value)
        }
        return cmp.apply(lhs.asDouble(), rhs.asDouble())
    }

    /**
     * Shared 2-or-3 arg "chain" evaluator used by `<` and `<=`.
     * `json-logic-js` declares the operator as `function(a, b, c)`:
     * missing operands resolve to `undefined` (NaN comparisons are
     * always `false`); the 3-arg form is the between-form
     * (`a < b AND b < c`); arguments past the third are dropped.
     */
    private fun evalChain(
        args: Value,
        vars: Value,
        cmp: Comparator,
    ): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val lhs = evaluated.firstOrNull()
        val mid = if (evaluated.size >= 2) evaluated[1] else null
        if (evaluated.size >= 3) {
            val rhs = evaluated[2]
            return Value.BoolValue(compare(lhs, mid, cmp) && compare(mid, rhs, cmp))
        }
        return Value.BoolValue(compare(lhs, mid, cmp))
    }

    /**
     * Shared 2-arg evaluator used by `>` and `>=`. `json-logic-js`
     * declares them as `function(a, b)`: extras are silently dropped
     * and a missing operand becomes NaN (which makes any comparison
     * `false`).
     */
    private fun evalBinary(
        args: Value,
        vars: Value,
        cmp: Comparator,
    ): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val lhs = evaluated.firstOrNull()
        val rhs = if (evaluated.size >= 2) evaluated[1] else null
        return Value.BoolValue(compare(lhs, rhs, cmp))
    }

    /**
     * Coerce to [Double], falling back to [Double.NaN] for non-numeric
     * operands. `null` means the argument was omitted → [Double.NaN]
     * (not a number), matching JS `Number(undefined)`.
     */
    private fun Value?.asDouble(): Double = this?.toNumberOrNull() ?: Double.NaN
}
