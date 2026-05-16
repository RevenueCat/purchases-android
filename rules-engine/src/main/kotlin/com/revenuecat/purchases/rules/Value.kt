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
 * JSON Logic loose equality (`==`). Best-effort JS-style coercion for the
 * common primitive cases. Arrays/objects compare structurally (deviates
 * from JS reference identity but is more useful for rule authors).
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

    if (lhs is Value.ArrayValue && rhs is Value.ArrayValue) {
        return lhs.items.size == rhs.items.size &&
            lhs.items.zip(rhs.items).all { (left, right) -> looseEq(left, right) }
    }
    if (lhs is Value.ObjectValue && rhs is Value.ObjectValue) {
        if (lhs.entries.size != rhs.entries.size) return false
        return lhs.entries.all { (key, value) ->
            val other = rhs.entries[key] ?: return@all false
            looseEq(value, other)
        }
    }

    val leftNumber = lhs.toNumberOrNull() ?: return false
    val rightNumber = rhs.toNumberOrNull() ?: return false
    return leftNumber == rightNumber
}

/**
 * JSON Logic strict equality (`===`). Same type, same value. Numeric
 * strict-eq treats `IntValue(1)` and `FloatValue(1.0)` as equal — they
 * represent the same JS `Number`, and our split is an internal modeling
 * choice.
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
    if (lhs is Value.ArrayValue && rhs is Value.ArrayValue) {
        return lhs.items.size == rhs.items.size &&
            lhs.items.zip(rhs.items).all { (left, right) -> strictEq(left, right) }
    }
    if (lhs is Value.ObjectValue && rhs is Value.ObjectValue) {
        if (lhs.entries.size != rhs.entries.size) return false
        return lhs.entries.all { (key, value) ->
            val other = rhs.entries[key] ?: return@all false
            strictEq(value, other)
        }
    }
    return false
}
