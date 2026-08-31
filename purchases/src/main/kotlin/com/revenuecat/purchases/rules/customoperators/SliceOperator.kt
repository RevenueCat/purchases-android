package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/** Name used in this operator's error messages. */
private const val OPERATOR_NAME = "rc.slice"

/** `rc.slice` — takes a run of elements out of an array. */
internal object SliceOperator {

    private const val BINARY_OPERAND_COUNT = 2
    private const val TERNARY_OPERAND_COUNT = 3

    /**
     * `{"rc.slice": [array, start]}` or
     * `{"rc.slice": [array, start, length]}`.
     *
     * Start and length mean what they mean in `substr`, the string operator
     * already in the engine: a negative `start` counts from the end, a
     * negative `length` drops that many elements from the right, and both
     * clamp to the array instead of failing. Strings keep using `substr`.
     *
     * Indices must be whole numbers — a float is accepted because all
     * arithmetic returns one, so `length - 1` is an ordinary way to reach the
     * last element. A fractional or non-numeric index throws
     * [EvaluationException.TypeMismatch], as does a non-array operand.
     */
    fun opSlice(args: Value, vars: Scope): Value {
        val evaluated = Operators.evalArgs(args, vars)

        if (evaluated.size != BINARY_OPERAND_COUNT && evaluated.size != TERNARY_OPERAND_COUNT) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expects 2 or 3 arguments, got ${evaluated.size}",
            )
        }

        val source = evaluated[0]
        if (source !is Value.ArrayValue) {
            val hint = if (source is Value.StringValue) "; strings use 'substr'" else ""
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expected an array to slice, got $source$hint",
            )
        }

        val items = source.items
        val start = index(evaluated[1], "start")
        val begin = if (start < 0) maxOf(items.size + start, 0) else minOf(start, items.size)
        val remaining = items.subList(begin, items.size).toList()

        if (evaluated.size != TERNARY_OPERAND_COUNT) return Value.ArrayValue(remaining)

        val length = index(evaluated[2], "length")
        val count = if (length < 0) {
            maxOf(remaining.size + length, 0)
        } else {
            minOf(length, remaining.size)
        }
        return Value.ArrayValue(remaining.subList(0, count).toList())
    }

    /** Reads a whole-number index, rejecting anything that is not one. */
    private fun index(value: Value, argument: String): Int = when {
        // Clamping rather than truncating keeps an index wider than Int
        // pointing past the same end of the array it does on iOS.
        value is Value.IntValue ->
            value.value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
        value is Value.FloatValue && value.value % 1.0 == 0.0 -> Operators.clampedInt(value.value)
        else -> throw EvaluationException.TypeMismatch(
            "operator '$OPERATOR_NAME' expected a whole number $argument, got $value",
        )
    }
}
