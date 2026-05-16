package com.revenuecat.purchases.rules.helpers

import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal

/**
 * Test-only convenience for converting a JSON literal into a [Value]. Lets
 * the tests express predicates the same way they appear in rule artifacts
 * instead of building the tree by hand.
 *
 * Production code never goes through this path: native callers will
 * construct [Value] trees from their own JSON parser when wiring this
 * engine to the SDK. Mirroring the Rust evaluator's `cfg(test)` JSON
 * helper, this lives in the test source set only.
 */
internal object ValueJsonHelper {

    fun fromJsonString(input: String): Value {
        return try {
            convert(JSONTokener(input).nextValue())
        } catch (e: JSONException) {
            throw RuleError.Parse(e.message ?: "unknown JSON error")
        }
    }

    private fun convert(parsed: Any?): Value = when (parsed) {
        null, JSONObject.NULL -> Value.Null
        is Boolean -> Value.BoolValue(parsed)
        is Int -> Value.IntValue(parsed.toLong())
        is Long -> Value.IntValue(parsed)
        is Double -> Value.FloatValue(parsed)
        is Float -> Value.FloatValue(parsed.toDouble())
        is BigDecimal -> {
            // JSONTokener returns BigDecimal for any literal containing a
            // decimal point (or an integer that doesn't fit `Long`). Use
            // `scale() > 0` as a proxy for "the source literal had a
            // decimal point" — matches the Rust evaluator's behaviour
            // where `100` deserializes as `Int` and `100.0` as `Float`.
            if (parsed.scale() > 0) {
                Value.FloatValue(parsed.toDouble())
            } else {
                Value.IntValue(parsed.toLong())
            }
        }
        is Number -> {
            // Catch-all for any other JSONTokener numeric type (e.g.
            // `BigInteger`). Bias toward `IntValue` when the number fits
            // in `Long` losslessly.
            val asLong = parsed.toLong()
            if (asLong.toDouble() == parsed.toDouble()) {
                Value.IntValue(asLong)
            } else {
                Value.FloatValue(parsed.toDouble())
            }
        }
        is String -> Value.StringValue(parsed)
        is JSONArray -> Value.ArrayValue(
            (0 until parsed.length()).map { convert(parsed.opt(it)) },
        )
        is JSONObject -> {
            val entries = mutableMapOf<String, Value>()
            val keys = parsed.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                entries[key] = convert(parsed.opt(key))
            }
            Value.ObjectValue(entries)
        }
        else -> Value.Null
    }
}
