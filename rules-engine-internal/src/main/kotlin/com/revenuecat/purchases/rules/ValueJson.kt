package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.RulesEngine.EvaluationError
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal

/**
 * Production JSON → [Value] parser. Converts the predicate JSON extracted
 * from the SDK artifact into the engine's typed [Value] tree. Used by
 * [RulesEngine.evaluate]; failures surface as [EvaluationError.Parse].
 */
internal object ValueJson {

    fun parse(input: String): Value {
        return try {
            convert(JSONTokener(input).nextValue())
        } catch (@Suppress("SwallowedException") e: JSONException) {
            throw EvaluationError.Parse(e.message ?: "unknown JSON error")
        }
    }

    @Suppress("CyclomaticComplexMethod")
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
        else -> throw EvaluationError.Parse(
            "unexpected JSONTokener output of type ${parsed::class.qualifiedName}",
        )
    }
}
