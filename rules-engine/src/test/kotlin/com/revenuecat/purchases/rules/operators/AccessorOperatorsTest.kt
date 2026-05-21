package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLoggerRule
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

class AccessorOperatorsTest {

    @get:Rule
    internal val loggerRule = CapturingLoggerRule()

    private val warnings: List<String> get() = loggerRule.warnings

    // ---- var ----

    @Test
    fun `var resolves top-level key`() {
        val vars = obj("name" to s("ada"))
        val out = AccessorOperators.opVar(s("name"), vars)
        assertThat(out).isEqualTo(s("ada"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var resolves dot-path into nested object`() {
        // {"subscriber": {"last_seen_country": "US"}}
        val vars = obj("subscriber" to obj("last_seen_country" to s("US")))
        val out = AccessorOperators.opVar(s("subscriber.last_seen_country"), vars)
        assertThat(out).isEqualTo(s("US"))
    }

    @Test
    fun `var indexes into arrays via numeric segments`() {
        val vars = obj(
            "items" to Value.ArrayValue(listOf(s("first"), s("second"), s("third"))),
        )
        val out = AccessorOperators.opVar(s("items.1"), vars)
        assertThat(out).isEqualTo(s("second"))
    }

    @Test
    fun `var missing key returns null and warns`() {
        val vars = obj("a" to Value.IntValue(1))
        val out = AccessorOperators.opVar(s("missing_key"), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("missing_key")
    }

    @Test
    fun `var missing dot-path returns null and warns`() {
        val vars = obj("a" to obj("b" to Value.IntValue(1)))
        val out = AccessorOperators.opVar(s("a.c"), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("a.c")
    }

    @Test
    fun `var missing with default returns default and does not warn`() {
        val vars = obj("a" to Value.IntValue(1))
        val result = AccessorOperators.opVar(
            Value.ArrayValue(listOf(s("missing"), s("fallback"))),
            vars,
        )
        assertThat(result).isEqualTo(s("fallback"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var empty path returns entire data`() {
        val vars = obj("x" to Value.IntValue(1))
        val out = AccessorOperators.opVar(s(""), vars)
        assertThat(out).isEqualTo(vars)
    }

    @Test
    fun `var with numeric path arg is coerced to string`() {
        // {"var": 0} on array data
        val vars = Value.ArrayValue(listOf(s("zero"), s("one")))
        val out = AccessorOperators.opVar(Value.IntValue(0), vars)
        assertThat(out).isEqualTo(s("zero"))
    }

    @Test
    fun `var with integer-valued float path looks up integer index`() {
        // {"var": 1.0} on array data must render as "1" (not "1.0") so the
        // path resolves to array index 1 — same lookup as `{"var": 1}`.
        val vars = Value.ArrayValue(listOf(s("zero"), s("one"), s("two")))
        val out = AccessorOperators.opVar(Value.FloatValue(1.0), vars)
        assertThat(out).isEqualTo(s("one"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var with fractional float path does not match adjacent indices`() {
        // {"var": 1.5} must not silently collapse to "1" or "2" — its
        // rendered path is "1.5", which doesn't resolve, so the lookup
        // misses and warns. Guards against an over-eager rounding fix to
        // `formatNumber`.
        val vars = Value.ArrayValue(listOf(s("zero"), s("one"), s("two")))
        val out = AccessorOperators.opVar(Value.FloatValue(1.5), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("1.5")
    }

    @Test
    fun `var with oversized float path does not crash`() {
        // `1e19` is a finite whole-number Double whose magnitude exceeds
        // Long.MAX_VALUE (~9.22e18). `formatNumber` must reject it via
        // the `value == value.toLong().toDouble()` guard so the path
        // formatter falls back to `Double.toString()` and the lookup just
        // misses.
        val out = AccessorOperators.opVar(Value.FloatValue(1.0e19), Value.Null)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
    }

    @Test
    fun `var recursively evaluates singleton path expression`() {
        // Per the JSON Logic spec, `{"var": <expr>}` recursively evaluates
        // <expr> and uses the result as the path. Here the inner
        // `{"var": "active_path_key"}` resolves to "subscriber.country",
        // which the outer var then looks up.
        val vars = obj(
            "active_path_key" to s("subscriber.country"),
            "subscriber" to obj("country" to s("US")),
        )
        val out = AccessorOperators.opVar(
            Value.ObjectValue(mapOf("var" to s("active_path_key"))),
            vars,
        )
        assertThat(out).isEqualTo(s("US"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var recursively evaluates array-form path expression`() {
        // Array form: the path argument is itself an expression that
        // resolves dynamically. Mirrors the json-logic-js per-element
        // evaluation rule for array args.
        val vars = obj(
            "key_to_lookup" to s("nested.value"),
            "nested" to obj("value" to s("found")),
        )
        val out = AccessorOperators.opVar(
            Value.ArrayValue(
                listOf(Value.ObjectValue(mapOf("var" to s("key_to_lookup")))),
            ),
            vars,
        )
        assertThat(out).isEqualTo(s("found"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var recursively evaluates array-form default expression`() {
        // The default arg in the array form is also recursively evaluated,
        // so callers can express dynamic fallbacks like
        // `{"var": ["missing_key", {"var": "fallback_source"}]}`.
        val vars = obj("fallback_source" to s("computed_default"))
        val out = AccessorOperators.opVar(
            Value.ArrayValue(
                listOf(
                    s("missing_key"),
                    Value.ObjectValue(mapOf("var" to s("fallback_source"))),
                ),
            ),
            vars,
        )
        assertThat(out).isEqualTo(s("computed_default"))
        // No missing-variable warning: the default short-circuited the lookup.
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var singleton expression resolving to array throws`() {
        // json-logic-js JS-stringifies a non-primitive evaluated result
        // ("x,y") and looks it up; we choose to be strict instead and
        // throw TypeMismatch so the malformed predicate surfaces loudly.
        val args = Value.ObjectValue(
            mapOf(
                "if" to Value.ArrayValue(
                    listOf(
                        Value.BoolValue(true),
                        Value.ArrayValue(listOf(s("x"), s("y"))),
                        s("z"),
                    ),
                ),
            ),
        )
        assertThatThrownBy { AccessorOperators.opVar(args, Value.Null) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    @Test
    fun `var does not apply flat-key fallback`() {
        // The literal key "a.b" exists in the flat map, but our spec-strict
        // lookup walks "a" then "b" and finds nothing. Documents the
        // deferred fallback behavior.
        val vars = obj("a.b" to Value.IntValue(42))
        val out = AccessorOperators.opVar(s("a.b"), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
    }

    @Test
    fun `var extra args are ignored with warning`() {
        // Reference impls silently ignore extras; we surface a warning so it
        // doesn't become a silent bug. Path + default still resolve normally.
        val vars = obj("a" to Value.IntValue(1))
        val out = AccessorOperators.opVar(
            Value.ArrayValue(
                listOf(
                    s("missing_key"),
                    s("fallback"),
                    s("ignored1"),
                    s("ignored2"),
                ),
            ),
            vars,
        )
        // Default kicks in (path is missing) — extras don't change the result.
        assertThat(out).isEqualTo(s("fallback"))
        // One warning for the extras; no missing-variable warning since the
        // default short-circuited the lookup.
        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("ignoring 2 extra")
    }

    // ---- missing ----

    @Test
    fun `missing returns keys not present`() {
        val vars = obj("a" to Value.IntValue(1), "b" to Value.IntValue(2))
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("a"), s("b"), s("c"))),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("c"))))
        // `missing` itself does not warn (it's a check, not a read).
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `missing returns empty array when all present`() {
        val vars = obj("a" to Value.IntValue(1))
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("a"))),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(emptyList()))
    }

    @Test
    fun `missing supports dot-path keys`() {
        val vars = obj("user" to obj("name" to s("ada")))
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("user.name"), s("user.email"))),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("user.email"))))
    }

    @Test
    fun `missing singleton shorthand is supported`() {
        val vars = obj()
        val result = AccessorOperators.opMissing(s("a"), vars)
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("a"))))
    }

    // ---- missing_some ----

    @Test
    fun `missing_some returns empty when threshold met`() {
        // need=1, options=[a, b, c]; b is present → 1 ≥ 1 → satisfied → [].
        val vars = obj("b" to Value.IntValue(2))
        val result = runMissingSome(
            Value.ArrayValue(
                listOf(
                    Value.IntValue(1),
                    Value.ArrayValue(listOf(s("a"), s("b"), s("c"))),
                ),
            ),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(emptyList()))
    }

    @Test
    fun `missing_some returns missing list when below threshold`() {
        // need=2, options=[a, b, c]; only c is present → 1 < 2 → list missing.
        val vars = obj("c" to Value.IntValue(3))
        val result = runMissingSome(
            Value.ArrayValue(
                listOf(
                    Value.IntValue(2),
                    Value.ArrayValue(listOf(s("a"), s("b"), s("c"))),
                ),
            ),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("a"), s("b"))))
    }

    @Test
    fun `missing_some zero required is always satisfied`() {
        // need=0 means "any number of these is fine" → always [].
        val result = runMissingSome(
            Value.ArrayValue(
                listOf(
                    Value.IntValue(0),
                    Value.ArrayValue(listOf(s("a"), s("b"))),
                ),
            ),
            obj(),
        )
        assertThat(result).isEqualTo(Value.ArrayValue(emptyList()))
    }

    @Test
    fun `missing_some supports dot-paths`() {
        // Mirrors `missing` semantics — path strings flow through the same
        // dot-walker.
        val vars = obj("user" to obj("name" to s("ada")))
        val result = runMissingSome(
            Value.ArrayValue(
                listOf(
                    Value.IntValue(2),
                    Value.ArrayValue(listOf(s("user.name"), s("user.email"), s("user.age"))),
                ),
            ),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("user.email"), s("user.age"))))
    }

    @Test
    fun `missing_some arity mismatch is type error`() {
        assertThatThrownBy {
            runMissingSome(Value.ArrayValue(listOf(Value.IntValue(1))), obj())
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    @Test
    fun `missing_some non-array options is type error`() {
        assertThatThrownBy {
            runMissingSome(
                Value.ArrayValue(listOf(Value.IntValue(1), s("a"))),
                obj(),
            )
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- missing — JSON Logic spec edge cases ----

    @Test
    fun `missing reports null-valued keys as missing per spec`() {
        // json-logic-js spec: a key with an explicit null value is "missing".
        // Our `lookupPath` distinguishes "absent" (Kotlin null) from "explicit
        // null" (Value.Null) — the spec collapses both into "missing".
        val vars = obj("a" to Value.Null, "b" to Value.IntValue(1))
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("a"), s("b"))),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("a"))))
    }

    @Test
    fun `missing reports empty-string-valued keys as missing per spec`() {
        // Same spec rule as the null case — `""` is treated as "no usable
        // value". Other falsy-but-defined values stay non-missing.
        val vars = obj("a" to s(""), "b" to s("present"))
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("a"), s("b"))),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("a"))))
    }

    @Test
    fun `missing does not report falsy-but-defined values as missing`() {
        // Pinning the negative side of the spec: only `null` and `""` qualify.
        // `0`, `false`, `[]`, `{}` are present-and-defined, hence not missing.
        val vars = obj(
            "zero" to Value.IntValue(0),
            "false_val" to Value.BoolValue(false),
            "empty_array" to Value.ArrayValue(emptyList()),
            "empty_object" to Value.ObjectValue(emptyMap()),
            "zero_string" to s("0"),
        )
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(
                listOf(s("zero"), s("false_val"), s("empty_array"), s("empty_object"), s("zero_string")),
            ),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(emptyList()))
    }

    @Test
    fun `missing recursively evaluates dynamic keys`() {
        // Per the JSON Logic spec, each key arg is recursively evaluated
        // before lookup. The inner `{"var": "key_name"}` resolves to
        // "absent", which `missing` then checks against `vars`.
        val vars = obj(
            "key_name" to s("absent"),
            "present_only" to Value.IntValue(1),
        )
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(
                listOf(Value.ObjectValue(mapOf("var" to s("key_name")))),
            ),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("absent"))))
    }

    @Test
    fun `missing unpacks first arg when it resolves to array`() {
        // Spec: if the first (possibly only) evaluated arg is itself an
        // array, treat its elements as the full key list. Here `if` returns
        // `["a", "c"]`, which `missing` unpacks before checking each key.
        val vars = obj("a" to Value.IntValue(1))
        val result = AccessorOperators.opMissing(
            Value.ObjectValue(
                mapOf(
                    "if" to Value.ArrayValue(
                        listOf(
                            Value.BoolValue(true),
                            Value.ArrayValue(listOf(s("a"), s("c"))),
                            Value.ArrayValue(emptyList()),
                        ),
                    ),
                ),
            ),
            vars,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("c"))))
    }

    // ---- helpers ----

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)

    private fun runMissingSome(args: Value, vars: Value): Value =
        AccessorOperators.opMissingSome(args, vars)
}
