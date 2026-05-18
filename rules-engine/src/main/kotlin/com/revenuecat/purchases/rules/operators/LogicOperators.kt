package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.Value

/**
 * Logic operators: `!`, `!!`, `and`, `or`, `if`.
 *
 * `and`, `or`, and `if` short-circuit and therefore can't use the eager
 * [Operators.evalArgs] helpers — they evaluate their arguments one at a
 * time.
 */
internal object LogicOperators {

    /**
     * `{"!": x}` — boolean negation. Coerces to bool first per JSON Logic
     * truthiness rules.
     */
    fun opNot(args: Value, vars: Value): Value {
        val value = firstArgEvaluated(args, vars)
        return Value.BoolValue(!value.isTruthy)
    }

    /** `{"!!": x}` — boolean cast. Spec: equivalent to `!!x` in JS. */
    fun opNotNot(args: Value, vars: Value): Value {
        val value = firstArgEvaluated(args, vars)
        return Value.BoolValue(value.isTruthy)
    }

    /**
     * `{"and": [a, b, c]}` — short-circuit AND. Returns the first falsy
     * value or, if all are truthy, the last value (matches JS / JSON Logic
     * semantics: `and` returns the actual value, not a coerced boolean).
     *
     * Empty args (`{"and": []}`) returns [Value.Null], matching the JS
     * reference impl whose `current` variable is left `undefined` when
     * the loop body never runs.
     */
    fun opAnd(args: Value, vars: Value): Value {
        var last: Value = Value.Null
        for (item in Operators.argsAsList(args)) {
            last = Evaluator.evaluateValue(item, vars)
            if (!last.isTruthy) return last
        }
        return last
    }

    /**
     * `{"or": [a, b, c]}` — short-circuit OR. Returns the first truthy
     * value or, if all are falsy, the last value. Empty args returns
     * [Value.Null] for the same reason as [opAnd].
     */
    fun opOr(args: Value, vars: Value): Value {
        var last: Value = Value.Null
        for (item in Operators.argsAsList(args)) {
            last = Evaluator.evaluateValue(item, vars)
            if (last.isTruthy) return last
        }
        return last
    }

    /**
     * `{"if": [cond, then, else]}` — also supports chained
     * `[c1, t1, c2, t2, ..., else]` (think `else if`). Without an `else`
     * clause and with no truthy condition, returns `Null`.
     */
    @Suppress("ReturnCount")
    fun opIf(args: Value, vars: Value): Value {
        val items = Operators.argsAsList(args)
        if (items.isEmpty()) return Value.Null
        var index = 0
        while (index + 1 < items.size) {
            val condition = Evaluator.evaluateValue(items[index], vars)
            if (condition.isTruthy) {
                return Evaluator.evaluateValue(items[index + 1], vars)
            }
            index += 2
        }
        return if (index < items.size) {
            Evaluator.evaluateValue(items[index], vars)
        } else {
            Value.Null
        }
    }

    private fun firstArgEvaluated(args: Value, vars: Value): Value {
        val items = Operators.argsAsList(args)
        val first = items.firstOrNull() ?: Value.Null
        return Evaluator.evaluateValue(first, vars)
    }
}
