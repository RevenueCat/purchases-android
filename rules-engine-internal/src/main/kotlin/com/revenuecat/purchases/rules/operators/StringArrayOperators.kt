package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsArrayElementString
import com.revenuecat.purchases.rules.jsString
import com.revenuecat.purchases.rules.strictEq

/**
 * String + array operators: `in`, `cat`, `substr`, `merge`.
 *
 * Behavior follows the JSON Logic JS reference (`json-logic-js`).
 * `substr` slices by Unicode code points, not UTF-16 code units —
 * matches Kotlin's `String.codePointCount` semantics; differs from JS
 * only for surrogate-pair characters.
 */
internal object StringArrayOperators {

    /**
     * `{"in": [needle, haystack]}` — substring or array-membership test.
     * For a [Value.StringValue] haystack, the needle is stringified and
     * the test is substring containment (mirrors JS
     * `String.prototype.indexOf`); an empty haystack is falsy in
     * json-logic-js (`if (!b) return false`), so `in` never matches. For a
     * [Value.ArrayValue] haystack, the test is strict element equality
     * (mirrors JS `Array.prototype.indexOf`, which uses `===`). Any other
     * haystack type returns `false`. `json-logic-js` declares `in` as
     * `function(a, b)`, so missing or extra operands short-circuit to
     * `false`.
     */
    fun opIn(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val needle = evaluated.firstOrNull() ?: Value.Null
        val haystack = if (evaluated.size >= 2) evaluated[1] else Value.Null
        val result = when (haystack) {
            is Value.StringValue -> {
                // json-logic-js: `if (!b || …) return false` — empty string is
                // falsy, so `in` never matches regardless of needle.
                if (haystack.value.isEmpty()) false
                else haystack.value.contains(jsString(needle))
            }
            is Value.ArrayValue -> haystack.items.any { strictEq(needle, it) }
            else -> false
        }
        return Value.BoolValue(result)
    }

    /**
     * `{"cat": [a, b, ...]}` — variadic string concatenation. Each
     * operand is rendered via [jsArrayElementString] (mirrors
     * `Array.prototype.join` on the argument list: `null` → `""`).
     * 0 args returns `""`.
     */
    fun opCat(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        return Value.StringValue(evaluated.joinToString(separator = "") { jsArrayElementString(it) })
    }

    /**
     * `{"substr": [source, start]}` or
     * `{"substr": [source, start, length]}`. `source` is stringified.
     * Negative `start` counts from the end. A negative `length` drops
     * that many code points from the right of the substring that starts
     * at `start`. Code-point-based, not char-based — see type docs.
     * `json-logic-js` declares `substr` as
     * `function(source, start, end)`, so a missing `start` defaults to
     * `0` and arguments past the third are silently ignored.
     */
    fun opSubstr(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val source = evaluated.firstOrNull() ?: Value.Null
        val start = if (evaluated.size >= 2) evaluated[1] else Value.Null
        val length = if (evaluated.size >= 3) evaluated[2] else null

        val codePoints = jsString(source).codePoints().toArray()
        val total = codePoints.size

        val startN = Operators.clampedInt(start.toNumberOrNull() ?: 0.0)
        val begin = if (startN < 0) {
            (total + startN).coerceAtLeast(0)
        } else {
            startN.coerceAtMost(total)
        }

        val afterStart = codePoints.copyOfRange(begin, codePoints.size)

        val resultPoints = if (length != null) {
            val lenN = Operators.clampedInt(length.toNumberOrNull() ?: 0.0)
            val count = if (lenN < 0) {
                (afterStart.size + lenN).coerceAtLeast(0)
            } else {
                lenN.coerceAtMost(afterStart.size)
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
    fun opMerge(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
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
}
