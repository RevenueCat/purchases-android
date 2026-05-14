package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.RulesEngineLogger
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.looseEq

/**
 * String + array operators: `in`, `cat`, `substr`, `merge`.
 *
 * Behavior follows the JSON Logic JS reference (`json-logic-js`) with
 * two deliberate, documented deviations:
 *
 * - **`in` array membership uses [looseEq]** (so `{"in": [5, ["5"]]}`
 *   is true) instead of the JS reference's strict `===`. Rule authors
 *   typically write integer literals against backend-supplied string
 *   lists, and loose equality is more forgiving for that workflow.
 *   Pure-string and pure-numeric comparisons are unaffected.
 * - **`substr` slices by Unicode code points**, not UTF-16 code units.
 *   Matches Kotlin's `String.codePointCount` semantics and gives the
 *   intuitive answer for multibyte strings; differs from JS only for
 *   surrogate-pair characters (rare in real rule data).
 */
internal object StringArrayOperators {

    private const val SUBSTR_MIN_ARITY = 2
    private const val SUBSTR_MAX_ARITY = 3

    /**
     * `{"in": [needle, haystack]}` — substring or array-membership test.
     * For a [Value.StringValue] haystack, the needle must also be a
     * string and the test is substring containment. For a
     * [Value.ArrayValue] haystack, the test is element membership via
     * [looseEq]. Any other haystack type returns `false` (mirrors JS,
     * where a non-`indexOf`-able haystack short-circuits to false).
     */
    fun opIn(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val (needle, haystack) = Operators.evalTwo(args, vars, logger, "in")
        val result = when {
            needle is Value.StringValue && haystack is Value.StringValue ->
                haystack.value.contains(needle.value)
            haystack is Value.ArrayValue ->
                haystack.items.any { looseEq(needle, it) }
            else -> false
        }
        return Value.BoolValue(result)
    }

    /**
     * `{"cat": [a, b, ...]}` — variadic string concatenation. Each
     * operand is stringified via [stringify]. 0 args returns `""`.
     */
    fun opCat(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        return Value.StringValue(evaluated.joinToString(separator = "") { stringify(it) })
    }

    /**
     * `{"substr": [source, start]}` or
     * `{"substr": [source, start, length]}`. `source` is stringified.
     * Negative `start` counts from the end. A negative `length` drops
     * that many code points from the right of the substring that starts
     * at `start` (matches the JS reference). Code-point-based, not
     * char-based — see type docs.
     */
    fun opSubstr(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        val source: Value
        val start: Value
        val length: Value?
        when (evaluated.size) {
            SUBSTR_MIN_ARITY -> {
                source = evaluated[0]
                start = evaluated[1]
                length = null
            }
            SUBSTR_MAX_ARITY -> {
                source = evaluated[0]
                start = evaluated[1]
                length = evaluated[2]
            }
            else -> throw RuleError.TypeMismatch(
                "operator 'substr' expects 2 or 3 arguments, got ${evaluated.size}",
            )
        }

        val codePoints = stringify(source).codePoints().toArray()
        val total = codePoints.size.toLong()

        // Non-numeric start coerces to 0 (mirrors JS:
        // `Number(undefined)` → NaN → treated as 0 by
        // `String.prototype.substr`).
        val startN = (start.toNumberOrNull() ?: 0.0).toLong()
        val begin: Int = if (startN < 0L) {
            (total + startN).coerceAtLeast(0L).toInt()
        } else {
            startN.coerceAtMost(total).toInt()
        }

        val afterStart = codePoints.copyOfRange(begin, codePoints.size)

        val resultPoints: IntArray = if (length != null) {
            val lenN = (length.toNumberOrNull() ?: 0.0).toLong()
            val count: Int = if (lenN < 0L) {
                (afterStart.size.toLong() + lenN).coerceAtLeast(0L).toInt()
            } else {
                lenN.coerceAtMost(afterStart.size.toLong()).toInt()
            }
            afterStart.copyOfRange(0, count)
        } else {
            afterStart
        }
        return Value.StringValue(String(resultPoints, 0, resultPoints.size))
    }

    /**
     * `{"merge": [a, b, ...]}` — variadic, flattens one level. Array
     * operands are spliced in; non-array operands are appended as
     * single elements.
     */
    fun opMerge(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        val merged = mutableListOf<Value>()
        for (item in evaluated) {
            if (item is Value.ArrayValue) {
                merged += item.items
            } else {
                merged += item
            }
        }
        return Value.ArrayValue(merged)
    }

    /**
     * Coerce a [Value] to a [String] for `cat` / `substr`. Mirrors
     * JavaScript's `String(value)`:
     * - `Null` → `"null"`
     * - `BoolValue` → `"true"` / `"false"`
     * - `IntValue` / `FloatValue` → numeric repr (integers render
     *   without a trailing `.0`, matching JS)
     * - `StringValue` → unchanged
     * - `ArrayValue` → comma-joined recursive stringify (JS
     *   `Array.prototype.toString`)
     * - `ObjectValue` → `"[object Object]"` (matches JS; concatenating
     *   an object is almost always a rule-authoring bug, but the
     *   result is at least defined)
     */
    private fun stringify(value: Value): String = when (value) {
        Value.Null -> "null"
        is Value.BoolValue -> value.value.toString()
        is Value.IntValue -> value.value.toString()
        is Value.FloatValue -> formatNumber(value.value)
        is Value.StringValue -> value.value
        is Value.ArrayValue -> value.items.joinToString(separator = ",") { stringify(it) }
        is Value.ObjectValue -> "[object Object]"
    }

    /**
     * Render a [Double] the way JS would — `1.0` becomes `"1"`, `1.5`
     * stays `"1.5"`.
     */
    private fun formatNumber(value: Double): String {
        if (value.isFinite() && value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        return value.toString()
    }
}
