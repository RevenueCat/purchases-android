package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationError
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsParseFloat

/**
 * Arithmetic operators: `+`, `-`, `*`, `/`, `%`.
 *
 * All operators return [Value.FloatValue]. `json-logic-js` is asymmetric
 * about which JS coercion each operator uses, and we replicate that:
 *
 * - `+` and `*` go through `parseFloat(value)` — `value` is stringified
 *   first, then the longest numeric prefix is parsed. `null`, bools,
 *   the empty string, and `[1,2]` all yield `NaN`; `"3.14abc"` parses
 *   as `3.14`. See [jsParseFloat].
 * - `-`, `/`, `%` use native JS arithmetic which calls `Number(value)`
 *   — bool / null / empty-string become `0`, arrays / objects coerce
 *   via `ToPrimitive("number")` → `toString` → recurse. `[]` → `0`,
 *   `[1]` → `1`, `[1,2]` → `NaN`. See [Value.toNumberOrNull].
 *
 * Division and modulo by zero produce the IEEE 754 values (`±Infinity`
 * for `n / 0` with `n ≠ 0`, `NaN` for `0 / 0` and any `n % 0`), matching
 * `json-logic-js`.
 */
internal object ArithmeticOperators {

    /**
     * `{"+": [a, b, ...]}` — variadic sum, seeded with `0`. 0 arguments
     * returns `0`. Each operand is coerced via JS `parseFloat`.
     */
    fun opAdd(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val sum = evaluated.fold(0.0) { acc, value -> acc + jsParseFloat(value) }
        return Value.FloatValue(sum)
    }

    /**
     * `{"*": [a, b, ...]}` — variadic product, no seed (matches
     * `Array.prototype.reduce` without an initial value). The 1-arg form
     * returns the operand unchanged (no `parseFloat` coercion). 0
     * arguments is a [EvaluationError.TypeMismatch] to mirror `[].reduce(fn)`
     * throwing.
     */
    fun opMul(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val head = evaluated.firstOrNull()
            ?: throw EvaluationError.TypeMismatch("operator '*' requires at least 1 argument")
        if (evaluated.size <= 1) return head
        val product = evaluated.drop(1).fold(jsParseFloat(head)) { acc, value ->
            acc * jsParseFloat(value)
        }
        return Value.FloatValue(product)
    }

    /**
     * `{"-": [a]}` — unary negation. `{"-": [a, b]}` — subtraction.
     * `{"-": [a, b, ...]}` ignores extra operands. `{"-": []}` returns
     * `NaN` (mirroring JS `-undefined`). Operands are coerced via JS
     * `Number()` ([Value.toNumberOrNull]).
     */
    fun opSub(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val lhs = evaluated.firstOrNull()?.asDouble() ?: Double.NaN
        return if (evaluated.size >= 2) {
            Value.FloatValue(lhs - evaluated[1].asDouble())
        } else {
            Value.FloatValue(-lhs)
        }
    }

    /**
     * `{"/": [a, b]}` — division. Extra operands are ignored; missing
     * operands resolve to `NaN` (mirroring JS `undefined / x`). Division
     * by zero follows IEEE 754: `n / 0` is `±Infinity`, `0 / 0` is `NaN`.
     */
    fun opDiv(args: Value, vars: Value): Value {
        val (lhs, rhs) = evalDivisorPair(args, vars)
        return Value.FloatValue(lhs / rhs)
    }

    /**
     * `{"%": [a, b]}` — modulo. Same arity / coercion rules as `/`;
     * `n % 0` follows IEEE 754 and is `NaN`.
     */
    fun opMod(args: Value, vars: Value): Value {
        val (lhs, rhs) = evalDivisorPair(args, vars)
        return Value.FloatValue(lhs % rhs)
    }

    /**
     * Evaluate two operands into [Double], defaulting missing operands
     * to [Double.NaN] (mirroring JS `undefined`). Extra operands are
     * ignored.
     */
    private fun evalDivisorPair(args: Value, vars: Value): Pair<Double, Double> {
        val evaluated = Operators.evalArgs(args, vars)
        val lhs = evaluated.firstOrNull()?.asDouble() ?: Double.NaN
        val rhs = if (evaluated.size >= 2) evaluated[1].asDouble() else Double.NaN
        return lhs to rhs
    }

    /**
     * `Number(value)`-style coercion for `-`, `/`, `%`. Falls back to
     * [Double.NaN] so arithmetic propagates the failure without raising
     * an error. `+` and `*` use [jsParseFloat] instead — see type docs.
     */
    private fun Value.asDouble(): Double = toNumberOrNull() ?: Double.NaN
}
