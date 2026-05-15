package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLogger
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class AccessorOperatorsTest {

    // ---- var ----

    @Test
    fun `var resolves top-level key`() {
        val vars = obj("name" to s("ada"))
        val (out, warnings) = runVar(s("name"), vars)
        assertThat(out).isEqualTo(s("ada"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var resolves dot-path into nested object`() {
        // {"subscriber": {"last_seen_country": "US"}}
        val vars = obj("subscriber" to obj("last_seen_country" to s("US")))
        val (out, _) = runVar(s("subscriber.last_seen_country"), vars)
        assertThat(out).isEqualTo(s("US"))
    }

    @Test
    fun `var indexes into arrays via numeric segments`() {
        val vars = obj(
            "items" to Value.ArrayValue(listOf(s("first"), s("second"), s("third"))),
        )
        val (out, _) = runVar(s("items.1"), vars)
        assertThat(out).isEqualTo(s("second"))
    }

    @Test
    fun `var missing key returns null and warns`() {
        val vars = obj("a" to Value.IntValue(1))
        val (out, warnings) = runVar(s("missing_key"), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("missing_key")
    }

    @Test
    fun `var missing dot-path returns null and warns`() {
        val vars = obj("a" to obj("b" to Value.IntValue(1)))
        val (out, warnings) = runVar(s("a.c"), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("a.c")
    }

    @Test
    fun `var missing with default returns default and does not warn`() {
        val vars = obj("a" to Value.IntValue(1))
        val logger = CapturingLogger()
        val result = AccessorOperators.opVar(
            Value.ArrayValue(listOf(s("missing"), s("fallback"))),
            vars,
            logger,
        )
        assertThat(result).isEqualTo(s("fallback"))
        assertThat(logger.warnings).isEmpty()
    }

    @Test
    fun `var empty path returns entire data`() {
        val vars = obj("x" to Value.IntValue(1))
        val (out, _) = runVar(s(""), vars)
        assertThat(out).isEqualTo(vars)
    }

    @Test
    fun `var with numeric path arg is coerced to string`() {
        // {"var": 0} on array data
        val vars = Value.ArrayValue(listOf(s("zero"), s("one")))
        val (out, _) = runVar(Value.IntValue(0), vars)
        assertThat(out).isEqualTo(s("zero"))
    }

    @Test
    fun `var does not apply flat-key fallback`() {
        // The literal key "a.b" exists in the flat map, but our spec-strict
        // lookup walks "a" then "b" and finds nothing. Documents the
        // deferred fallback behavior.
        val vars = obj("a.b" to Value.IntValue(42))
        val (out, warnings) = runVar(s("a.b"), vars)
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).hasSize(1)
    }

    @Test
    fun `var extra args are ignored with warning`() {
        // Reference impls silently ignore extras; we surface a warning so it
        // doesn't become a silent bug. Path + default still resolve normally.
        val vars = obj("a" to Value.IntValue(1))
        val (out, warnings) = runVar(
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
        val logger = CapturingLogger()
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("a"), s("b"), s("c"))),
            vars,
            logger,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("c"))))
        // `missing` itself does not warn (it's a check, not a read).
        assertThat(logger.warnings).isEmpty()
    }

    @Test
    fun `missing returns empty array when all present`() {
        val vars = obj("a" to Value.IntValue(1))
        val logger = CapturingLogger()
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("a"))),
            vars,
            logger,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(emptyList()))
    }

    @Test
    fun `missing supports dot-path keys`() {
        val vars = obj("user" to obj("name" to s("ada")))
        val logger = CapturingLogger()
        val result = AccessorOperators.opMissing(
            Value.ArrayValue(listOf(s("user.name"), s("user.email"))),
            vars,
            logger,
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("user.email"))))
    }

    @Test
    fun `missing singleton shorthand is supported`() {
        val vars = obj()
        val logger = CapturingLogger()
        val result = AccessorOperators.opMissing(s("a"), vars, logger)
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

    // ---- helpers ----

    private fun runVar(pathArg: Value, vars: Value): Pair<Value, List<String>> {
        val logger = CapturingLogger()
        val result = AccessorOperators.opVar(pathArg, vars, logger)
        return result to logger.warnings
    }

    private fun runMissingSome(args: Value, vars: Value): Value =
        AccessorOperators.opMissingSome(args, vars, CapturingLogger())

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
