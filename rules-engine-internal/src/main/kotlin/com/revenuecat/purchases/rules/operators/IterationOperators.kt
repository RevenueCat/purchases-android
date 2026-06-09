package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.Value

/**
 * Iteration operators: `some`, `all`, `none`, `map`, `filter`, `reduce`.
 *
 * All six follow the JSON Logic JS reference (`json-logic-js`):
 *
 * - **Shape** (`some` / `all` / `none` / `map` / `filter`):
 *   `{"<op>": [arrayExpr, predicateExpr]}`. The first argument is
 *   evaluated in the outer scope and must resolve to an array; anything
 *   else is treated as an empty source. The second argument is a
 *   literal template that is evaluated per-item with `vars` rebound to
 *   the current item, with no parent-scope inheritance.
 * - **Shape** (`reduce`):
 *   `{"reduce": [arrayExpr, predicateExpr, initialAccumulator]}`. Both
 *   the first and third arguments are evaluated in the outer scope.
 *   The predicate is evaluated per-item with `vars` rebound to
 *   `{"current": <item>, "accumulator": <acc>}`, with no parent-scope
 *   inheritance.
 *
 * **Empty- and non-array sources** per the JSON Logic JS spec:
 * - `some` / `all` return `false`.
 * - `none` returns `true`.
 * - `map` / `filter` return `[]`.
 * - `reduce` returns the initial accumulator unchanged.
 */
internal object IterationOperators {

    /**
     * `{"some": [arrayExpr, predicate]}` — `true` iff `predicate` is
     * truthy for at least one item. The array expression is evaluated in
     * the current scope; the predicate is re-evaluated per item with
     * `vars` rebound to that item, with no parent-scope inheritance.
     * Empty array or non-array source returns `false`. Short-circuits on
     * the first truthy result.
     */
    fun opSome(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val result = items?.any { Evaluator.evaluateValue(predicate, it).isTruthy } ?: false
        return Value.BoolValue(result)
    }

    /**
     * `{"all": [arrayExpr, predicate]}` — `true` iff `predicate` is
     * truthy for every item. The array expression is evaluated in the
     * current scope; the predicate is re-evaluated per item with `vars`
     * rebound to that item, with no parent-scope inheritance. Empty array
     * returns `false` per the JSON Logic JS spec. Non-array source
     * returns `false`. Short-circuits on the first non-truthy result.
     */
    fun opAll(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val result = !items.isNullOrEmpty() &&
            items.all { Evaluator.evaluateValue(predicate, it).isTruthy }
        return Value.BoolValue(result)
    }

    /**
     * `{"none": [arrayExpr, predicate]}` — `true` iff `predicate` is
     * falsy for every item. Inverse of `some`. Short-circuits on the
     * first truthy item. Empty and non-array sources both return `true`,
     * matching the JS reference's `!Array.isArray(x) || !x.length` guard.
     */
    fun opNone(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val result = items?.none { Evaluator.evaluateValue(predicate, it).isTruthy } ?: true
        return Value.BoolValue(result)
    }

    /**
     * `{"map": [arrayExpr, predicate]}` — apply `predicate` to each
     * item, return the new array of *raw* (non-truthy-coerced) results.
     * Empty or non-array source yields `[]`.
     */
    fun opMap(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val results = items?.map { Evaluator.evaluateValue(predicate, it) } ?: emptyList()
        return Value.ArrayValue(results)
    }

    /**
     * `{"filter": [arrayExpr, predicate]}` — return only items for
     * which `predicate` is truthy. Empty or non-array source yields
     * `[]`. The retained items are the *original* values, not the
     * predicate results.
     */
    fun opFilter(args: Value, vars: Value): Value {
        val (items, predicate) = parseIterationArgs(args, vars)
        val results = items?.filter { Evaluator.evaluateValue(predicate, it).isTruthy } ?: emptyList()
        return Value.ArrayValue(results)
    }

    /**
     * `{"reduce": [arrayExpr, predicate, initialAccumulator]}` — fold
     * over the array. The third argument is evaluated in the outer
     * scope to seed the accumulator, then the predicate is evaluated
     * once per item with `vars` rebound to
     * `{"current": item, "accumulator": acc}`. A non-array source
     * returns the seed unchanged. A missing initial accumulator
     * defaults to [Value.Null] and arguments past the third are ignored.
     */
    fun opReduce(args: Value, vars: Value): Value {
        val raw = Operators.argsAsList(args)
        val sourceArg = raw.getOrNull(0) ?: Value.Null
        val predicate = raw.getOrNull(1) ?: Value.Null
        val source = Evaluator.evaluateValue(sourceArg, vars)
        var accumulator = raw.getOrNull(2)?.let { Evaluator.evaluateValue(it, vars) } ?: Value.Null
        val items = (source as? Value.ArrayValue)?.items ?: return accumulator
        for (item in items) {
            val scope = Value.ObjectValue(mapOf("current" to item, "accumulator" to accumulator))
            accumulator = Evaluator.evaluateValue(predicate, scope)
        }
        return accumulator
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
