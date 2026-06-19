package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsString

/**
 * Comparison operators: `<`, `<=`, `>`, `>=`.
 *
 * Per JSON Logic:
 *
 * - After `ToPrimitive` (number hint), **both operands are strings**
 *   → lexicographic comparison (`"10" < "9"` is `true`). Arrays and
 *   objects stringify first (`[] < "a"` → `"" < "a"` → `true`).
 * - **Otherwise** → coerce both operands to [Double] via
 *   [Value.toNumberOrNull] and compare numerically (`"10" < 9` →
 *   `false`). Unparseable strings compared numerically become
 *   [Double.NaN]; per IEEE 754 every comparison against NaN returns
 *   `false`.
 *
 * `<` and `<=` accept a 3-arg "between" form per the JSON Logic spec:
 * `{"<": [a, b, c]}` reads as `a < b AND b < c`. `>` and `>=` are
 * binary only.
 */
internal object ComparisonOperators {

    private const val BINARY_OPERAND_COUNT = 2
    private const val BETWEEN_OPERAND_COUNT = 3
    private const val MID_OPERAND_INDEX = 1
    private const val RHS_OPERAND_INDEX = 2

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
     * Mirrors JS `<`: `ToPrimitive` (number hint), lex when both are
     * strings, else numeric. `null` lhs/rhs is an omitted argument
     * (`undefined` → `NaN` → `false`).
     */
    private fun compare(lhs: Value?, rhs: Value?, cmp: Comparator): Boolean = when {
        lhs == null || rhs == null -> cmp.apply(lhs.asDouble(), rhs.asDouble())
        else -> {
            val left = toPrimitiveForComparison(lhs)
            val right = toPrimitiveForComparison(rhs)
            when {
                left is Value.StringValue && right is Value.StringValue ->
                    cmp.apply(left.value, right.value)
                else -> cmp.apply(left.asDouble(), right.asDouble())
            }
        }
    }

    /** `ToPrimitive` with number hint: arrays/objects stringify. */
    private fun toPrimitiveForComparison(value: Value): Value = when (value) {
        is Value.StringValue,
        Value.Null,
        is Value.BoolValue,
        is Value.IntValue,
        is Value.FloatValue,
        -> value
        is Value.ArrayValue,
        is Value.ObjectValue,
        -> Value.StringValue(jsString(value))
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
        val mid = if (evaluated.size >= BINARY_OPERAND_COUNT) {
            evaluated[MID_OPERAND_INDEX]
        } else {
            null
        }
        if (evaluated.size >= BETWEEN_OPERAND_COUNT) {
            val rhs = evaluated[RHS_OPERAND_INDEX]
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
        val rhs = if (evaluated.size >= BINARY_OPERAND_COUNT) {
            evaluated[MID_OPERAND_INDEX]
        } else {
            null
        }
        return Value.BoolValue(compare(lhs, rhs, cmp))
    }

    /** Omitted arg or failed coercion → [Double.NaN]; [Value.Null] → 0. */
    private fun Value?.asDouble(): Double = this?.toNumberOrNull() ?: Double.NaN
}
