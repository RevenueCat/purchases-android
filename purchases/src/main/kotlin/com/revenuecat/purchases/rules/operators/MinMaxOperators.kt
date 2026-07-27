package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Value
import kotlin.math.max
import kotlin.math.min

/**
 * Numeric extrema: `min`, `max`.
 *
 * Variadic over a flat list of values, matching JS `Math.max` / `Math.min`. Each
 * operand goes through [Value.toNumberOrNull] (JS `Number()`), so single-element arrays
 * bridge to a number via `toString` (`Math.max(1, [2]) === 2`); multi-element arrays,
 * objects, and unparseable strings become NaN and poison the result.
 *
 * Empty input mirrors `Math.max()` / `Math.min()`: max → -∞, min → +∞.
 */
internal object MinMaxOperators {

    fun opMax(args: Value, vars: Value): Value {
        return reduceExtremum(args, vars, empty = Double.NEGATIVE_INFINITY) { a, b -> max(a, b) }
    }

    fun opMin(args: Value, vars: Value): Value {
        return reduceExtremum(args, vars, empty = Double.POSITIVE_INFINITY) { a, b -> min(a, b) }
    }

    private fun reduceExtremum(
        args: Value,
        vars: Value,
        empty: Double,
        combine: (Double, Double) -> Double,
    ): Value {
        val result = Operators.evalArgs(args, vars).fold(empty) { accumulator, value ->
            val number = value.toNumberOrNull() ?: Double.NaN
            if (accumulator.isNaN() || number.isNaN()) {
                Double.NaN
            } else {
                combine(accumulator, number)
            }
        }
        return Value.FloatValue(result)
    }
}
