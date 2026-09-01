package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsArrayElementString
import com.revenuecat.purchases.rules.jsString
import com.revenuecat.purchases.rules.strictEq

/**
 * String + array operators: `in`, `cat`, `substr`, `merge`.
 *
 * Behavior follows the JSON Logic JS reference (`json-logic-js`).
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
    fun opIn(args: Value, vars: Scope): Value {
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
    fun opCat(args: Value, vars: Scope): Value {
        val evaluated = Operators.evalArgs(args, vars)
        return Value.StringValue(evaluated.joinToString(separator = "") { jsArrayElementString(it) })
    }

    /**
     * `{"substr": [source, start]}` or
     * `{"substr": [source, start, length]}`. `source` is stringified.
     * Negative `start` counts from the end. A negative `length` drops
     * that many code units from the right of the substring that starts
     * at `start`. `json-logic-js` declares `substr` as
     * `function(source, start, end)`, so a missing `start` defaults to
     * `0` and arguments past the third are silently ignored. A missing
     * `source` is `undefined`, which stringifies to `"undefined"` (not
     * `"null"`).
     *
     * Indices are **UTF-16 code units**, because `json-logic-js` is
     * `String.prototype.substr` and that is the unit JS strings are indexed
     * in. Kotlin's [String] is already a code-unit sequence, so a slice that
     * lands inside a surrogate pair keeps the half pair exactly as JS does.
     * A Swift `String` cannot, so `purchases-ios` yields U+FFFD there —
     * same code-unit count, different content.
     */
    fun opSubstr(args: Value, vars: Scope): Value {
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

        val text = jsString(source)
        val total = text.length

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
            text.substring(begin, begin + count)
        } else {
            text.substring(begin, total)
        }
        return Value.StringValue(result)
    }

    /**
     * `{"merge": [a, b, ...]}` — variadic, flattens one level. Array
     * operands are spliced in; non-array operands are appended as
     * single elements.
     */
    fun opMerge(args: Value, vars: Scope): Value {
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
