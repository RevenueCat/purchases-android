package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.RulesEngineLog
import com.revenuecat.purchases.rules.Value

/**
 * `var` and `missing` — the data-accessor operators.
 */
internal object AccessorOperators {

    /**
     * `{"var": "subscriber.last_seen_country"}` — look up a (possibly
     * nested) value by dot-path. `{"var": ["path", default]}` returns
     * `default` when the path is missing. `{"var": ""}` returns the entire
     * data scope.
     *
     * The path argument is treated as a literal — it is NOT recursively
     * evaluated as a JSON Logic expression. So a construct like
     * `{"var": {"cat": ["subscriber.", {"var": "field_name"}]}}` (which
     * some other implementations support) is rejected here.
     *
     * Variable lookup uses **strict JSON Logic dot-path semantics on
     * nested objects**. There is no flat-key fallback (i.e. we do not also
     * try the literal dotted string as a single key in the top-level map).
     */
    @Suppress("ReturnCount")
    fun opVar(args: Value, vars: Value): Value {
        val (path, default) = parseVarArgs(args)

        if (path.isEmpty()) {
            return vars
        }

        val found = lookupPath(vars, path)
        if (found != null) return found
        if (default != null) return default
        RulesEngineLog.warn("missing variable: $path")
        return Value.Null
    }

    /**
     * `{"missing": ["a", "b.c"]}` returns the array of keys (as strings)
     * that are NOT present in the data. Returns `[]` when nothing is
     * missing.
     */
    fun opMissing(args: Value, vars: Value): Value {
        val keys: List<Value> = when (args) {
            is Value.ArrayValue -> args.items
            // Singleton shorthand: `{"missing": "a"}` ≡ `{"missing": ["a"]}`.
            else -> listOf(args)
        }

        val missing = mutableListOf<Value>()
        for (key in keys) {
            val path = keyAsPath(key) ?: continue
            if (lookupPath(vars, path) == null) {
                missing += Value.StringValue(path)
            }
        }
        return Value.ArrayValue(missing)
    }

    /**
     * Normalize `var`'s arg into a `(path, default)` pair. Accepts a
     * string/number literal, `Null` (= empty path), or `[path, default?]`.
     */
    private fun parseVarArgs(args: Value): Pair<String, Value?> = when (args) {
        Value.Null -> "" to null
        is Value.StringValue -> args.value to null
        is Value.IntValue -> args.value.toString() to null
        is Value.FloatValue -> formatNumber(args.value) to null
        is Value.ArrayValue -> parseVarArrayArgs(args.items)
        else -> throw RuleError.TypeMismatch(
            "var arg must be a string, number, or array, got $args",
        )
    }

    private fun parseVarArrayArgs(items: List<Value>): Pair<String, Value?> {
        val path = pathSegment(items.firstOrNull())
        val default = if (items.size >= 2) items[1] else null
        if (items.size > 2) {
            RulesEngineLog.warn(
                "var: ignoring ${items.size - 2} extra arg(s); expected [path] or [path, default]",
            )
        }
        return path to default
    }

    private fun pathSegment(value: Value?): String = when (value) {
        null, Value.Null -> ""
        is Value.StringValue -> value.value
        is Value.IntValue -> value.value.toString()
        is Value.FloatValue -> formatNumber(value.value)
        else -> throw RuleError.TypeMismatch(
            "var path must be a string or number, got $value",
        )
    }

    private fun keyAsPath(value: Value): String? = when (value) {
        is Value.StringValue -> value.value
        is Value.IntValue -> value.value.toString()
        is Value.FloatValue -> formatNumber(value.value)
        else -> null
    }

    /**
     * Walk [vars] following [path] (dot-separated). Numeric segments index
     * into arrays; string segments key into objects. Returns `null` if any
     * segment can't resolve.
     */
    @Suppress("ReturnCount")
    private fun lookupPath(vars: Value, path: String): Value? {
        var current: Value = vars
        for (segment in path.split(".")) {
            current = when (val node = current) {
                is Value.ObjectValue -> node.entries[segment] ?: return null
                is Value.ArrayValue -> {
                    val idx = segment.toIntOrNull() ?: return null
                    if (idx < 0 || idx >= node.items.size) return null
                    node.items[idx]
                }
                else -> return null
            }
        }
        return current
    }

    /**
     * Render a [Double] the way JSON Logic / JS would — `1.0` becomes
     * `"1"`, `1.5` stays `"1.5"`. Used so a numeric path like `var: 1.0`
     * looks up `"1"` (i.e. array index 1), not `"1.0"`.
     */
    private fun formatNumber(value: Double): String {
        if (value.isFinite() && value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        return value.toString()
    }
}
