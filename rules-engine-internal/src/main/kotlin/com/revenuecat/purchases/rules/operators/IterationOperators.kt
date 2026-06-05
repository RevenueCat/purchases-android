package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.Value

/**
 * Iteration predicates: `some`, `all`.
 *
 * Both follow the JSON Logic JS reference (`json-logic-js`). Shape is
 * `{"some": [arrayExpr, predicateExpr]}` /
 * `{"all": [arrayExpr, predicateExpr]}`. The first argument is evaluated
 * in the current scope and must resolve to an array; anything else
 * short-circuits to `false`. The second argument is a literal template
 * that is evaluated per-item with `vars` rebound to the current item,
 * with no parent-scope inheritance.
 *
 * **Empty-array behavior**: `all` over an empty array returns `false`,
 * not vacuous truth, per the JSON Logic JS spec.
 */
internal object IterationOperators {

    /**
     * `{"some": [arrayExpr, predicate]}` — `true` iff `predicate` is
     * truthy for at least one item in the array. Empty array or
     * non-array source returns `false`. Short-circuits on the first
     * truthy result.
     */
    fun opSome(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val result = items?.any { Evaluator.evaluateValue(predicate, it).isTruthy } ?: false
        return Value.BoolValue(result)
    }

    /**
     * `{"all": [arrayExpr, predicate]}` — `true` iff `predicate` is
     * truthy for every item. Empty array returns `false` per the JSON
     * Logic JS spec, not vacuous truth. Non-array source returns
     * `false`. Short-circuits on the first non-truthy result.
     */
    fun opAll(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val result = !items.isNullOrEmpty() &&
            items.all { Evaluator.evaluateValue(predicate, it).isTruthy }
        return Value.BoolValue(result)
    }

    /**
     * Parse `(items, predicate)` for an iteration operator. The source
     * argument is evaluated in the outer scope; the predicate template
     * is returned unevaluated so the caller can re-evaluate it per item
     * with the item as scope. `items` is `null` when the source does not
     * resolve to an array, so callers can distinguish a non-array source
     * from a genuinely empty one (`some`/`all` treat both as `false`, but
     * `none`/`map`/`filter`/`reduce` need the distinction). A missing
     * predicate defaults to [Value.Null] and arguments past the second
     * are ignored, matching `json-logic-js`'s
     * `function(scopedData, scopedLogic)` signature.
     */
    private fun parseIterationArgs(
        args: Value,
        vars: Value,
    ): Pair<List<Value>?, Value> {
        val raw = Operators.argsAsList(args)
        val sourceArg = raw.getOrNull(0) ?: Value.Null
        val predicate = raw.getOrNull(1) ?: Value.Null
        val source = Evaluator.evaluateValue(sourceArg, vars)
        val items = (source as? Value.ArrayValue)?.items
        return items to predicate
    }
}
