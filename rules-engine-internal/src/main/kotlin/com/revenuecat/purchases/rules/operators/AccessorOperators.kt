package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsString
import com.revenuecat.purchases.rules.jsToNumber

/**
 * `var` and `missing` — the data-accessor operators.
 */
@Suppress("TooManyFunctions")
internal object AccessorOperators {

    /**
     * `{"var": "subscriber.last_seen_country"}` — look up a (possibly
     * nested) value by dot-path. `{"var": ["path", default]}` returns
     * `default` when the path is missing. `{"var": ""}` returns the entire
     * data scope.
     *
     * Per the JSON Logic spec, the path argument is recursively evaluated
     * before lookup, so callers can compute paths dynamically — e.g.
     * `{"var": {"var": "active_path_key"}}` resolves `active_path_key`
     * first and uses its string value as the actual path. In the array
     * form, the default argument is evaluated the same way.
     *
     * Variable lookup uses **strict JSON Logic dot-path semantics on
     * nested objects**. There is no flat-key fallback (i.e. we do not also
     * try the literal dotted string as a single key in the top-level map).
     */
    @Suppress("ReturnCount")
    fun opVar(args: Value, vars: Value): Value {
        val (path, default) = resolveVarArgs(args, vars)
        val found = lookupVar(vars, path)
        if (found != null) return found
        if (default != null) return default
        RulesEngine.logger.warn("missing variable: $path")
        return Value.Null
    }

    /**
     * `{"missing": ["a", "b.c"]}` returns the array of keys (as strings)
     * that are NOT present in the data. Returns `[]` when nothing is
     * missing.
     *
     * Per the JSON Logic spec:
     * - A key is "missing" when its resolved value is `null` OR `""` —
     *   i.e. the spec deliberately conflates "absent", "explicit null",
     *   and "empty string" into a single "no usable value" bucket. Other
     *   falsy values (`0`, `false`, `[]`) are NOT missing.
     * - Each key argument is recursively evaluated before lookup, so
     *   dynamic key lists like `{"missing": [{"var": "key_to_check"}]}`
     *   work.
     * - If the first (possibly only) evaluated argument is itself an
     *   array (typically the output of another operator), its elements
     *   are unpacked as the key list — this is how
     *   `{"missing": {"merge": [["a"], ["b"]]}}` is meant to behave.
     */
    fun opMissing(args: Value, vars: Value): Value {
        val evaluatedArgs: List<Value> = when (args) {
            is Value.ArrayValue -> args.items.map { Evaluator.evaluateValue(it, vars) }
            // Singleton shorthand: `{"missing": "a"}` ≡ `{"missing": ["a"]}`.
            else -> listOf(Evaluator.evaluateValue(args, vars))
        }

        val first = evaluatedArgs.firstOrNull()
        val keys: List<Value> = if (first is Value.ArrayValue) first.items else evaluatedArgs

        val missing = mutableListOf<Value>()
        for (key in keys) {
            val path = keyAsPath(key) ?: continue
            if (isMissing(varLookup(vars, path))) {
                missing += Value.StringValue(path)
            }
        }
        return Value.ArrayValue(missing)
    }

    /**
     * `{"missing_some": [min_required, [path, ...]]}` returns the
     * missing-keys array (same shape as `missing`) IF fewer than
     * `min_required` of the requested paths are present. Otherwise
     * returns `[]` (the rule's required-data condition is satisfied).
     * Used to express "any 2 of these 5 fields must be present" style
     * requirements.
     */
    fun opMissingSome(args: Value, vars: Value): Value {
        val evaluated = Operators.evalArgs(args, vars)
        if (evaluated.size != 2) {
            throw EvaluationException.TypeMismatch(
                "operator 'missing_some' expects 2 arguments, got ${evaluated.size}",
            )
        }
        val needCountValue = evaluated[0]

        // json-logic-js computes `missing.apply(this, [options])` for the
        // keys, then reads `options.length` for the threshold. So the key
        // set and the threshold count come from *different* views of
        // `options`:
        //   - array  → its elements are the keys; length = element count
        //   - string → the *whole string* is a single key; length = its
        //              UTF-16 code-unit count (Kotlin `String.length`, which
        //              matches JS `String.length`), so a long string can
        //              satisfy a larger threshold while only ever
        //              contributing one key
        //   - null   → no keys; `length` is `undefined`, which makes the
        //              threshold comparison NaN-based (always false), so the
        //              missing list is returned unconditionally
        //   - other  → `Function.prototype.apply` throws a `TypeError`
        val keys: List<Value>
        val total: Long?
        when (val options = evaluated[1]) {
            is Value.ArrayValue -> {
                keys = options.items
                total = options.items.size.toLong()
            }
            is Value.StringValue -> {
                keys = listOf(Value.StringValue(options.value))
                total = options.value.length.toLong()
            }
            Value.Null, Value.Undefined -> {
                keys = emptyList()
                total = null
            }
            else -> throw EvaluationException.TypeMismatch(
                "operator 'missing_some': second argument must be array-like, got $options",
            )
        }

        // Threshold uses JS `ToNumber` + `>=`. `NaN` and unparseable
        // strings never satisfy; `+Infinity` never satisfies for finite
        // present counts; `-Infinity` always satisfies.
        val need = jsToNumber(needCountValue)

        val missing = opMissing(Value.ArrayValue(keys), vars)
        val missingCount = (missing as? Value.ArrayValue)?.items?.size?.toLong() ?: 0L

        return if (total != null && (total - missingCount).toDouble() >= need) {
            Value.ArrayValue(emptyList())
        } else {
            missing
        }
    }

    /**
     * Spec-equivalent of `value === null || value === ""` after resolving
     * a path through `var`. `null` (Kotlin) means the key isn't in the
     * data at all; [Value.Null] means it's there with an explicit null
     * value; an empty [Value.StringValue] means it's there with `""`.
     */
    private fun isMissing(value: Value?): Boolean = when (value) {
        null, Value.Null -> true
        is Value.StringValue -> value.value.isEmpty()
        else -> false
    }

    /**
     * Recursively evaluate `var`'s arg(s) per the JSON Logic spec, then
     * normalize the result into a `(path, default)` pair. The array form
     * evaluates each element in place (so both path and default become
     * dynamic); the singleton form evaluates the (conceptually wrapped)
     * lone argument so that constructs like `{"var": {"var": "key"}}`
     * resolve to a dynamic path string.
     *
     */
    private fun resolveVarArgs(args: Value, vars: Value): Pair<String, Value?> {
        if (args is Value.ArrayValue) {
            val evaluated = args.items.map { Evaluator.evaluateValue(it, vars) }
            return parseVarArrayArgs(evaluated)
        }
        val evaluated = Evaluator.evaluateValue(args, vars)
        return pathSegment(evaluated) to null
    }

    private fun parseVarArrayArgs(items: List<Value>): Pair<String, Value?> {
        val path = pathSegment(items.firstOrNull())
        val default = if (items.size >= 2) items[1] else null
        if (items.size > 2) {
            RulesEngine.logger.warn(
                "var: ignoring ${items.size - 2} extra arg(s); expected [path] or [path, default]",
            )
        }
        return path to default
    }

    /**
     * Coerce the evaluated path argument to a string per
     * `json-logic-js`'s `String(a).split(".")`. `null`, [Value.Null],
     * [Value.Undefined], and `""` are treated as the empty path, which
     * signals the caller to return the entire data scope — matching
     * json-logic-js's `typeof a === "undefined" || a === "" || a === null`
     * guard.
     */
    private fun pathSegment(value: Value?): String = when (value) {
        null, Value.Null, Value.Undefined -> ""
        else -> jsString(value)
    }

    private fun keyAsPath(value: Value): String? {
        if (value is Value.Null) return null
        return jsString(value)
    }

    /**
     * Resolve [path] the way `var` does. Empty path returns the entire
     * data scope; a resolving path returns its value (including explicit
     * [Value.Null]); a non-resolving path returns `null`.
     */
    private fun lookupVar(vars: Value, path: String): Value? {
        if (path.isEmpty()) return vars
        return lookupPath(vars, path)
    }

    /**
     * Like [lookupVar], but maps a non-resolving path to [Value.Null]
     * instead of `null` — the shape `missing` needs.
     */
    private fun varLookup(vars: Value, path: String): Value =
        lookupVar(vars, path) ?: Value.Null

    /**
     * Mirrors `String(path).split(".")` in json-logic-js — preserves empty
     * segments at the start, middle, and end (unlike Kotlin's default split).
     */
    private fun jsDotSplit(path: String): List<String> {
        if (path.isEmpty()) return listOf("")
        val segments = mutableListOf<String>()
        var start = 0
        for (i in path.indices) {
            if (path[i] == '.') {
                segments.add(path.substring(start, i))
                start = i + 1
            }
        }
        segments.add(path.substring(start))
        return segments
    }

    /**
     * Walk [vars] following [path] (dot-separated). Numeric segments index
     * into arrays; string segments key into objects. Returns `null` if any
     * segment can't resolve.
     */
    @Suppress("ReturnCount")
    private fun lookupPath(vars: Value, path: String): Value? {
        var current: Value = vars
        for (segment in jsDotSplit(path)) {
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
}
