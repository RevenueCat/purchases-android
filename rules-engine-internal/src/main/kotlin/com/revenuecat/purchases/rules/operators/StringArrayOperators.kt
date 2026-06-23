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

    private const val BINARY_OPERAND_COUNT = 2
    private const val TERNARY_OPERAND_COUNT = 3
    private const val START_OPERAND_INDEX = 1
    private const val LENGTH_OPERAND_INDEX = 2

    /**
     * `{"in": [needle, haystack]}` — substring or array-membership test.
     * For a [Value.StringValue] haystack, the needle is stringified and
     * the test is substring containment (mirrors JS
     * `String.prototype.indexOf`); when the haystack is falsy,
     * json-logic-js returns false immediately
     * (`if (!haystack) return false`), so an empty string never matches. For a
     * [Value.ArrayValue] haystack, the test is strict element equality
     * (mirrors JS `Array.prototype.indexOf`, which uses `===`). Any other
     * haystack type returns `false`. `json-logic-js` implements `in` as
     * `function(a, b)` (needle, haystack); missing or extra operands
     * short-circuit to `false`.
     */
    fun opIn(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val needle = evaluated.firstOrNull() ?: Value.Null
        val haystack = if (evaluated.size >= BINARY_OPERAND_COUNT) {
            evaluated[START_OPERAND_INDEX]
        } else {
            Value.Null
        }
        val result = when (haystack) {
            is Value.StringValue -> {
                // json-logic-js: `if (!haystack || …) return false` — empty
                // string is falsy, so `in` never matches regardless of needle.
                if (haystack.value.isEmpty()) {
                    false
                } else {
                    haystack.value.contains(jsString(needle))
                }
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
     * `0` and arguments past the third are silently ignored. A missing
     * `source` is `undefined`, which stringifies to `"undefined"` (not
     * `"null"`).
     */
    fun opSubstr(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        val source = evaluated.firstOrNull() ?: Value.Undefined
        val start = if (evaluated.size >= BINARY_OPERAND_COUNT) {
            evaluated[START_OPERAND_INDEX]
        } else {
            Value.Null
        }
        val length = if (evaluated.size >= TERNARY_OPERAND_COUNT) {
            evaluated[LENGTH_OPERAND_INDEX]
        } else {
            null
        }

        val codePoints = jsString(source).toCodePointArray()
        val total = codePoints.size

        val startN = Operators.clampedInt(start.toNumberOrNull() ?: 0.0)
        val begin = if (startN < 0) {
            (total + startN).coerceAtLeast(0)
        } else {
            startN.coerceAtMost(total)
        }

        val afterStartLength = total - begin

        val result = if (length != null) {
            val lenN = Operators.clampedInt(length.toNumberOrNull() ?: 0.0)
            val count = if (lenN < 0) {
                (afterStartLength + lenN).coerceAtLeast(0)
            } else {
                lenN.coerceAtMost(afterStartLength)
            }
            codePoints.toStringFromCodePoints(begin, begin + count)
        } else {
            codePoints.toStringFromCodePoints(begin, total)
        }
        return Value.StringValue(result)
    }

    /**
     * Unicode code points without [String.codePoints] (API 24+).
     */
    private fun String.toCodePointArray(): IntArray {
        val points = mutableListOf<Int>()
        var index = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            points.add(codePoint)
            index += Character.charCount(codePoint)
        }
        return points.toIntArray()
    }

    private fun IntArray.toStringFromCodePoints(start: Int, end: Int): String {
        val builder = StringBuilder()
        for (index in start until end) {
            builder.appendCodePoint(this[index])
        }
        return builder.toString()
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
