package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsString
import com.revenuecat.purchases.rules.operators.Operators

/**
 * `rc.entries` and `rc.fromEntries` — convert between keyed objects and the
 * `[key, value]` pair arrays that JSON Logic iteration operators accept.
 */
internal object EntriesOperators {

    /**
     * `{"rc.entries": value}` — convert a keyed object or array into an array
     * of two-element `[key, value]` arrays so `some`, `filter`, etc. can
     * iterate it.
     *
     * **Argument form**: unary operators follow `json-logic-js` spread
     * semantics via [Operators.firstArgEvaluated] — a multi-element top-level
     * array is a multi-argument call, not a single array operand. So
     * `{"rc.entries": ["a", "b"]}` uses only `"a"`. To pass a literal array
     * operand, wrap it: `{"rc.entries": [["a", "b"]]}`.
     *
     * - **Object**: pairs sorted **lexicographically by key**, so both native
     *   SDKs emit the same order whether or not the underlying map preserves
     *   insertion order. This deliberately diverges from JS `Object.entries`
     *   insertion order; rules must not depend on insertion order.
     * - **Array**: index/value pairs with **string** keys (`"0"`, `"1"`, …),
     *   matching `Object.entries(["a","b"]) === [["0","a"],["1","b"]]`.
     * - **Anything else** (null, undefined, bool, number, string): throws
     *   [EvaluationException.TypeMismatch].
     *
     * Strings throw rather than yielding the character pairs JS
     * `Object.entries("ab")` produces. A string reaching here means the rule
     * points at the wrong field, and iterating its characters would let that
     * rule keep evaluating to a confident wrong answer.
     */
    fun opEntries(args: Value, vars: Scope): Value =
        when (val input = Operators.firstArgEvaluated(args, vars)) {
            is Value.ObjectValue -> Value.ArrayValue(
                input.entries.keys.sorted().map { key ->
                    Value.ArrayValue(listOf(Value.StringValue(key), input.entries[key] ?: Value.Null))
                },
            )

            is Value.ArrayValue -> Value.ArrayValue(
                input.items.mapIndexed { index, value ->
                    Value.ArrayValue(listOf(Value.StringValue(index.toString()), value))
                },
            )

            else -> throw EvaluationException.TypeMismatch(
                "operator 'rc.entries' expected object or array, got $input",
            )
        }

    /**
     * `{"rc.fromEntries": pairs}` — inverse of [opEntries].
     *
     * **Argument form**: same spread semantics as [opEntries]. A literal pair
     * list must be wrapped as a single operand, e.g.
     * `{"rc.fromEntries": [[["a", 1], ["b", 2]]]}`. Extra arguments are
     * silently ignored, matching `!` and `!!`.
     *
     * - **Array of two-element arrays**: builds an object. Keys are coerced
     *   via [jsString] (same helper `var` uses for path segments).
     * - **Duplicate keys**: last occurrence wins, matching JS.
     * - **Empty array**: `{}`.
     * - **Non-array argument**, or any entry that is not a two-element array:
     *   throws [EvaluationException.TypeMismatch].
     *
     * Malformed entries throw rather than being skipped. Dropping them would
     * build a partial object that reads as a legitimate result, so a rule
     * could match on the surviving keys alone.
     */
    fun opFromEntries(args: Value, vars: Scope): Value {
        val input = Operators.firstArgEvaluated(args, vars)

        if (input !is Value.ArrayValue) {
            throw EvaluationException.TypeMismatch(
                "operator 'rc.fromEntries' expected array, got $input",
            )
        }

        val result = mutableMapOf<String, Value>()
        for (entry in input.items) {
            if (entry !is Value.ArrayValue || entry.items.size != 2) {
                throw EvaluationException.TypeMismatch(
                    "operator 'rc.fromEntries' expected a two-element [key, value] entry, got $entry",
                )
            }

            result[jsString(entry.items[0])] = entry.items[1]
        }
        return Value.ObjectValue(result)
    }
}
