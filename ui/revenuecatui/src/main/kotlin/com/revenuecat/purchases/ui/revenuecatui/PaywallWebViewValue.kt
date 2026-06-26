package com.revenuecat.purchases.ui.revenuecatui

import org.json.JSONArray
import org.json.JSONObject

/**
 * A JSON-compatible value used in messages exchanged with a Paywalls V2 `web_view` component.
 *
 * Only JSON-like values are supported: [String], [Number], [Boolean], [Object], [Array] and [Null].
 * Functions, binary blobs, dates, and platform-specific objects are intentionally not representable.
 *
 * @see PaywallWebViewMessage
 * @see PaywallWebViewController
 */
// Modeled as an abstract class with a closed set of subtypes (mirroring [CustomVariableValue]) rather
// than a sealed class, to stay binary-compatible as public API.
public abstract class PaywallWebViewValue internal constructor() {

    /**
     * A string value.
     */
    public class String(public val value: kotlin.String) : PaywallWebViewValue() {
        override fun equals(other: Any?): kotlin.Boolean = other is String && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): kotlin.String = "PaywallWebViewValue.String(value=$value)"
    }

    /**
     * A numeric value (integer or decimal). Stored as a [Double].
     */
    public class Number(public val value: kotlin.Double) : PaywallWebViewValue() {
        public constructor(value: kotlin.Int) : this(value.toDouble())
        public constructor(value: kotlin.Long) : this(value.toDouble())
        public constructor(value: kotlin.Float) : this(value.toDouble())

        override fun equals(other: Any?): kotlin.Boolean = other is Number && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): kotlin.String = "PaywallWebViewValue.Number(value=$value)"
    }

    /**
     * A boolean value.
     */
    public class Boolean(public val value: kotlin.Boolean) : PaywallWebViewValue() {
        override fun equals(other: Any?): kotlin.Boolean = other is Boolean && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): kotlin.String = "PaywallWebViewValue.Boolean(value=$value)"
    }

    /**
     * A JSON object: a map of string keys to [PaywallWebViewValue]s.
     */
    public class Object(public val value: Map<kotlin.String, PaywallWebViewValue>) : PaywallWebViewValue() {
        override fun equals(other: Any?): kotlin.Boolean = other is Object && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): kotlin.String = "PaywallWebViewValue.Object(value=$value)"
    }

    /**
     * A JSON array: an ordered list of [PaywallWebViewValue]s.
     */
    public class Array(public val value: List<PaywallWebViewValue>) : PaywallWebViewValue() {
        override fun equals(other: Any?): kotlin.Boolean = other is Array && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): kotlin.String = "PaywallWebViewValue.Array(value=$value)"
    }

    /**
     * A JSON `null` value.
     */
    public object Null : PaywallWebViewValue() {
        override fun toString(): kotlin.String = "PaywallWebViewValue.Null"
    }

    internal companion object {
        /**
         * Converts an `org.json` value (or Kotlin primitive) into a [PaywallWebViewValue], rejecting
         * anything that is not JSON-compatible or that exceeds [remainingDepth] levels of nesting.
         *
         * @return the converted value, or `null` if the value is not JSON-compatible or too deeply nested.
         */
        @Suppress("ReturnCount", "CyclomaticComplexMethod")
        fun fromJson(value: Any?, remainingDepth: Int): PaywallWebViewValue? {
            return when (value) {
                null, JSONObject.NULL -> Null
                is kotlin.String -> String(value)
                is kotlin.Boolean -> Boolean(value)
                is Int -> Number(value)
                is Long -> Number(value)
                is Double -> Number(value)
                is Float -> Number(value)
                is JSONObject -> {
                    if (remainingDepth <= 0) return null
                    val map = LinkedHashMap<kotlin.String, PaywallWebViewValue>(value.length())
                    for (key in value.keys()) {
                        map[key] = fromJson(value.get(key), remainingDepth - 1) ?: return null
                    }
                    Object(map)
                }
                is JSONArray -> {
                    if (remainingDepth <= 0) return null
                    val list = ArrayList<PaywallWebViewValue>(value.length())
                    for (index in 0 until value.length()) {
                        list.add(fromJson(value.get(index), remainingDepth - 1) ?: return null)
                    }
                    Array(list)
                }
                // Numbers stored by org.json as BigDecimal/BigInteger, or any other type, are not supported.
                else -> (value as? kotlin.Number)?.let { Number(it.toDouble()) }
            }
        }
    }
}

/**
 * Converts this value into a representation accepted by [JSONObject]/[JSONArray] when serializing a
 * message to send into the web view. Whole numbers are emitted as integers (e.g. `100`, not `100.0`).
 */
internal fun PaywallWebViewValue.toJsonRepresentation(): Any = when (this) {
    is PaywallWebViewValue.String -> value
    is PaywallWebViewValue.Boolean -> value
    is PaywallWebViewValue.Number ->
        if (value % 1.0 == 0.0 && !value.isInfinite()) value.toLong() else value
    is PaywallWebViewValue.Object -> JSONObject().apply {
        value.forEach { (key, child) -> put(key, child.toJsonRepresentation()) }
    }
    is PaywallWebViewValue.Array -> JSONArray().apply {
        value.forEach { child -> put(child.toJsonRepresentation()) }
    }
    else -> JSONObject.NULL
}

internal fun Map<String, PaywallWebViewValue>.toJsonObject(): JSONObject = JSONObject().apply {
    forEach { (key, value) -> put(key, value.toJsonRepresentation()) }
}
