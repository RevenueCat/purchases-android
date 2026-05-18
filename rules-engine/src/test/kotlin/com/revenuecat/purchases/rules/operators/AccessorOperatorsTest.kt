package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLoggerRule
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
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

    // ---- helpers ----

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
