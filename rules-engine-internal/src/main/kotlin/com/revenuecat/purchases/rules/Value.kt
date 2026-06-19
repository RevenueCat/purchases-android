package com.revenuecat.purchases.rules

/**
 * A JSON-shaped value for JSON Logic predicates and variable data.
 *
 * Numbers are split into [IntValue] and [FloatValue] to preserve type intent.
 * Cross-type numeric comparisons still work — see [looseEq] and [strictEq].
 */
public sealed class Value {

    /** JSON `null`. */
    public object Null : Value()

    /** A JSON boolean. */
    public data class BoolValue(val value: Boolean) : Value()

    /** A JSON integer-valued number. */
    public data class IntValue(val value: Long) : Value()

    /** A JSON fractional (non-integer) number. */
    public data class FloatValue(val value: Double) : Value()

    /** A JSON string. */
    public data class StringValue(val value: String) : Value()

    /** A JSON array. */
    public data class ArrayValue(val items: List<Value>) : Value()

    /** A JSON object. */
    public data class ObjectValue(val entries: Map<String, Value>) : Value()

    /**
     * JSON Logic truthiness rules:
     * - `Null`, `false`, `0`, `""`, `[]`, `NaN` → falsy
     * - `ObjectValue(_)` → always truthy
     * - everything else → truthy
     */
    internal val isTruthy: Boolean
        get() = when (this) {
            Null -> false
            is BoolValue -> value
            is IntValue -> value != 0L
            is FloatValue -> value != 0.0 && !value.isNaN()
            is StringValue -> value.isNotEmpty()
            is ArrayValue -> items.isNotEmpty()
            is ObjectValue -> true
        }

    /**
     * JS `Number(value)` (`ToNumber`): bool→0/1, int/float→self, null→0,
     * string→`""` or whitespace→0 / parsed-or-`null`, array/object →
     * `ToPrimitive("number")` → `toString` → recurse on the resulting
     * string. So `[]` → `""` → 0, `[1]` → `"1"` → 1, `[1,2]` → `"1,2"` →
     * `null` (whole-string parse fails), `{}` → `"[object Object]"` →
     * `null`.
     *
     * Returning `null` rather than [Double.NaN] lets [looseEq]'s numeric
     * fallback distinguish "no comparable number" from "the number NaN"
     * (NaN compares unequal to itself, so a `null` short-circuit avoids
     * an accidental `false` masking a genuine spec match). Arithmetic
     * callers wrap with `?: Double.NaN` to get the JS arithmetic
     * propagation.
     */
    internal fun toNumberOrNull(): Double? = when (this) {
        Null -> 0.0
        is BoolValue -> if (value) 1.0 else 0.0
        is IntValue -> value.toDouble()
        is FloatValue -> value
        is StringValue -> {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) 0.0 else trimmed.toDoubleOrNull()
        }
        is ArrayValue, is ObjectValue -> {
            val trimmed = jsString(this).trim()
            if (trimmed.isEmpty()) 0.0 else trimmed.toDoubleOrNull()
        }
    }
}

/**
 * JS `ToNumber` for numeric comparisons (`>=`, etc.). Unlike
 * [toNumberOrNull], unparseable strings and compound values yield
 * [Double.NaN] so relational comparisons fail per the spec.
 */
internal fun jsToNumber(value: Value): Double = when (value) {
    Value.Null -> 0.0
    is Value.BoolValue -> if (value.value) 1.0 else 0.0
    is Value.IntValue -> value.value.toDouble()
    is Value.FloatValue -> value.value
    is Value.StringValue -> {
        val trimmed = value.value.trim()
        if (trimmed.isEmpty()) 0.0 else trimmed.toDoubleOrNull() ?: Double.NaN
    }
    is Value.ArrayValue, is Value.ObjectValue -> Double.NaN
}

/**
 * JSON Logic loose equality (`==`). Mirrors JS abstract equality:
 *
 * - Same-type primitive comparisons are direct value equality.
 * - Cross-numeric (`IntValue` ↔ `FloatValue`) bridges as one number type.
 * - **Compound vs compound**: always `false`. JS uses reference identity
 *   for arrays/objects; we have no references, so we mirror the
 *   literal-vs-literal result (`[1] == [1]` → `false`,
 *   `{a:1} == {a:1}` → `false`).
 * - **Compound vs primitive**: applies JS abstract equality's
 *   `ToPrimitive(string-hint)` step. Arrays render via
 *   `Array.prototype.toString()` (recursive comma-join, with
 *   `null` / `undefined` elements rendered as the empty string);
 *   objects render as `"[object Object]"`. So `[1] == "1"`, `[1, 2]
 *   == "1,2"`, `[null, 1] == ",1"`, and `[] == 0` all return `true`.
 * - **Last-resort numeric fallback**: when two primitives don't share
 *   a type, both sides are coerced to `Double` (JS `ToNumber`) and
 *   compared. Returns `false` if either coercion fails.
 */
@Suppress("ReturnCount", "ComplexMethod")
internal fun looseEq(lhs: Value, rhs: Value): Boolean {
    if (lhs is Value.Null && rhs is Value.Null) return true
    if (lhs is Value.Null || rhs is Value.Null) return false

    if (lhs is Value.BoolValue && rhs is Value.BoolValue) return lhs.value == rhs.value
    if (lhs is Value.StringValue && rhs is Value.StringValue) return lhs.value == rhs.value

    if (lhs is Value.IntValue && rhs is Value.IntValue) return lhs.value == rhs.value
    if (lhs is Value.FloatValue && rhs is Value.FloatValue) return lhs.value == rhs.value
    if (lhs is Value.IntValue && rhs is Value.FloatValue) return lhs.value.toDouble() == rhs.value
    if (lhs is Value.FloatValue && rhs is Value.IntValue) return lhs.value == rhs.value.toDouble()

    // Compound-vs-compound is reference equality in JS; without references
    // the only spec-aligned answer for two distinct operands is `false`.
    if (lhs is Value.ArrayValue && rhs is Value.ArrayValue) return false
    if (lhs is Value.ObjectValue && rhs is Value.ObjectValue) return false
    if (lhs is Value.ArrayValue && rhs is Value.ObjectValue) return false
    if (lhs is Value.ObjectValue && rhs is Value.ArrayValue) return false

    // JS abstract-equality coercion: when one side is a compound (Array
    // or Object) and the other is a primitive, ToPrimitive(string-hint)
    // the compound and re-compare. Order matters — compound-vs-compound
    // cases above must match first.
    if (lhs is Value.ArrayValue) return looseEq(Value.StringValue(jsArrayJoin(lhs.items)), rhs)
    if (rhs is Value.ArrayValue) return looseEq(lhs, Value.StringValue(jsArrayJoin(rhs.items)))
    if (lhs is Value.ObjectValue) return looseEq(Value.StringValue(JS_OBJECT_STRING), rhs)
    if (rhs is Value.ObjectValue) return looseEq(lhs, Value.StringValue(JS_OBJECT_STRING))

    val leftNumber = lhs.toNumberOrNull() ?: return false
    val rightNumber = rhs.toNumberOrNull() ?: return false
    return leftNumber == rightNumber
}

// ---- JS coercion helpers (used by looseEq, arithmetic, and stringifying operators) ----

/**
 * JS `String(value)`: `null` → `"null"`, booleans → `"true"` /
 * `"false"`, numbers → numeric repr (whole-valued doubles render
 * without a decimal, `NaN` / `±Infinity` keep their JS spellings),
 * strings unchanged, arrays via `Array.prototype.join(",")` (where
 * `null` elements render as the empty string), objects as
 * `"[object Object]"`.
 *
 * Differs from [jsArrayElementString] only in `null` handling:
 * `String(null) === "null"`, but `Array.prototype.join` renders
 * `null` / `undefined` array elements as the empty string. Use this
 * for callers that need the top-level `toString` (numeric coercion,
 * `parseFloat`-style stringification).
 */
internal fun jsString(value: Value): String = when (value) {
    Value.Null -> "null"
    is Value.BoolValue -> if (value.value) "true" else "false"
    is Value.IntValue -> value.value.toString()
    is Value.FloatValue -> jsNumberString(value.value)
    is Value.StringValue -> value.value
    is Value.ArrayValue -> jsArrayJoin(value.items)
    is Value.ObjectValue -> JS_OBJECT_STRING
}

/**
 * JS `parseFloat(value)`. Stringifies via [jsString], strips leading
 * whitespace, then parses the longest valid prefix as a JS
 * `StringNumericLiteral` (optional sign, digits with optional decimal,
 * optional decimal exponent, plus the `Infinity` literal). Anything else
 * — including `null` ("null"), bools ("true" / "false"), and the empty
 * string — yields [Double.NaN]. Lenient about trailing junk (`"3.14abc"`
 * → 3.14) to match JS's prefix-parsing behavior, which is what `+` /
 * `*` use in `json-logic-js`.
 */
@Suppress("ReturnCount")
internal fun jsParseFloat(value: Value): Double {
    if (value is Value.IntValue) return value.value.toDouble()
    if (value is Value.FloatValue) return value.value
    return parseFloatPrefix(jsString(value))
}

@Suppress("ReturnCount")
private fun parseFloatPrefix(string: String): Double {
    val trimmed = string.trimStart { it.isWhitespace() }
    if (trimmed.isEmpty()) return Double.NaN
    if (trimmed.startsWith("Infinity")) return Double.POSITIVE_INFINITY
    if (trimmed.startsWith("-Infinity")) return Double.NEGATIVE_INFINITY
    if (trimmed.startsWith("+Infinity")) return Double.POSITIVE_INFINITY
    val match = NUMERIC_PREFIX_REGEX.find(trimmed) ?: return Double.NaN
    if (match.range.first != 0) return Double.NaN
    return match.value.toDoubleOrNull() ?: Double.NaN
}

private val NUMERIC_PREFIX_REGEX = Regex("""^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?""")

/**
 * `Array.prototype.toString()` ≡ `Array.prototype.join(",")`. Renders each
 * element via [jsArrayElementString], then comma-joins.
 */
private fun jsArrayJoin(items: List<Value>): String =
    items.joinToString(",") { jsArrayElementString(it) }

/**
 * JS `Array.prototype.join` element rendering: `null` / `undefined`
 * render as the empty string (not `"null"`); everything else uses
 * [jsString].
 */
internal fun jsArrayElementString(value: Value): String {
    if (value is Value.Null) return ""
    return jsString(value)
}

/**
 * JS `String(number)` for the cases that show up in real rule data:
 * whole-number doubles render without a decimal (`String(1.0) === "1"`),
 * `NaN` / `±Infinity` keep their JS spellings, fractional doubles use
 * Kotlin's default rendering (matches JS for non-pathological values).
 *
 * Known divergence from JS: for values beyond exact integer round-trip range,
 * we fall through to Kotlin's `Double.toString()`, which may use scientific
 * notation earlier than JS (`1e19` → `"1.0E19"` vs `"10000000000000000000"`).
 * This only surfaces through `var` path coercion or `looseEq`'s compound-vs-
 * primitive arm with pathological magnitudes.
 */
@Suppress("ReturnCount")
private fun jsNumberString(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    val asLong = value.toLong()
    if (asLong.toDouble() == value) return asLong.toString()
    return value.toString()
}

/**
 * JS `Object.prototype.toString.call(plainObject)` for any non-Array
 * object. JSON Logic only ever encounters plain objects, so the fallback
 * `"[object Object]"` is the only spelling we need.
 */
private const val JS_OBJECT_STRING = "[object Object]"

/**
 * JSON Logic strict equality (`===`). Same type, same value. `IntValue(1)`
 * and `FloatValue(1.0)` compare equal — they represent the same JS
 * `Number`. Arrays and objects always compare unequal (JS reference
 * identity; see [looseEq] for the same rationale).
 */
@Suppress("ReturnCount", "ComplexMethod")
internal fun strictEq(lhs: Value, rhs: Value): Boolean {
    if (lhs is Value.Null && rhs is Value.Null) return true
    if (lhs is Value.BoolValue && rhs is Value.BoolValue) return lhs.value == rhs.value
    if (lhs is Value.IntValue && rhs is Value.IntValue) return lhs.value == rhs.value
    if (lhs is Value.FloatValue && rhs is Value.FloatValue) return lhs.value == rhs.value
    if (lhs is Value.IntValue && rhs is Value.FloatValue) return lhs.value.toDouble() == rhs.value
    if (lhs is Value.FloatValue && rhs is Value.IntValue) return lhs.value == rhs.value.toDouble()
    if (lhs is Value.StringValue && rhs is Value.StringValue) return lhs.value == rhs.value
    return false
}
