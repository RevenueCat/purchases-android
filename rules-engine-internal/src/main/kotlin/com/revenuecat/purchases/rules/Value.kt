package com.revenuecat.purchases.rules

/**
 * A JSON-shaped value. Used both as the parsed JSON Logic predicate tree
 * and as the resolved variable map handed in by callers.
 *
 * Maps directly onto the JSON data model with one tweak: numbers are split
 * into [IntValue] (`Long`) and [FloatValue] (`Double`) so callers can
 * preserve type intent. Cross-type numeric comparisons / arithmetic still
 * work — see [looseEq], [strictEq], and the comparison helpers below.
 *
 * JSON parsing intentionally lives only in tests (see the
 * `ValueJsonHelper` test helper). Production callers will cross any future
 * FFI boundary with a typed `Value` tree they construct from the host
 * SDK's JSON parser.
 *
 * Subtypes use structural [equals]/[hashCode] (via `data class` / the
 * [Null] singleton), so [Value] instances can be stored in [HashSet] for
 * O(1) membership checks when the upcoming `in` operator lands.
 */
internal sealed class Value {

    object Null : Value()
    data class BoolValue(val value: Boolean) : Value()
    data class IntValue(val value: Long) : Value()
    data class FloatValue(val value: Double) : Value()
    data class StringValue(val value: String) : Value()
    data class ArrayValue(val items: List<Value>) : Value()
    data class ObjectValue(val entries: Map<String, Value>) : Value()

    /**
     * JSON Logic truthiness rules:
     * - `Null`, `false`, `0`, `""`, `[]`, `NaN` → falsy
     * - `ObjectValue(_)` → always truthy
     * - everything else → truthy
     */
    val isTruthy: Boolean
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
     * Best-effort numeric coercion used by loose comparison. Mirrors JS
     * `ToNumber` (partial): bool→0/1, int/float→self, string→parsed (or
     * `null` if unparseable), null→0, everything else→`null`.
     */
    fun toNumberOrNull(): Double? = when (this) {
        Null -> 0.0
        is BoolValue -> if (value) 1.0 else 0.0
        is IntValue -> value.toDouble()
        is FloatValue -> value
        is StringValue -> {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) 0.0 else trimmed.toDoubleOrNull()
        }
        is ArrayValue, is ObjectValue -> null
    }
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

// ---- JS coercion helpers (used by looseEq and stringifying operators) ----

/**
 * JS `String(value)`: `null` → `"null"`, booleans → `"true"` /
 * `"false"`, numbers → numeric repr (whole-valued doubles render
 * without a decimal, `NaN` / `±Infinity` keep their JS spellings),
 * strings unchanged, arrays via `Array.prototype.join(",")` (where
 * `null` elements render as the empty string), objects as
 * `"[object Object]"`.
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
private fun jsArrayElementString(value: Value): String {
    if (value is Value.Null) return ""
    return jsString(value)
}

/**
 * JS `String(number)` for the cases that show up in real rule data:
 * whole-number doubles render without a decimal (`String(1.0) === "1"`),
 * `NaN` / `±Infinity` keep their JS spellings, fractional doubles use
 * Kotlin's default rendering (matches JS for non-pathological values).
 *
 * Known divergence: for `|value| ≥ Long.MAX_VALUE` (or any non-Long-roundtripping
 * Double) we fall through to Kotlin's `Double.toString()`, which uses scientific
 * notation earlier than JS does (`1e19` → `"1.0E19"` here vs `"10000000000000000000"`
 * in JS). iOS has a different but also off-spec rendering for the same input. The
 * divergence only surfaces through `var` path coercion or `looseEq`'s compound-vs-
 * primitive arm with pathological magnitudes; if conformance fixtures ever start
 * exercising those shapes we can implement ECMA-262 ToString(Number) properly.
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
