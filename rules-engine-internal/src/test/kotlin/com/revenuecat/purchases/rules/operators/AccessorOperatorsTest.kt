package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLoggerRule
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * The cases below are kept as Kotlin tests because they are not covered by
 * the shared JSON predicate fixtures (which evaluate a predicate to a
 * boolean). Either they cannot be expressed as a predicate → boolean — a
 * top-level *array* or *null* scope (`Evaluator.evaluate` always takes a
 * `Map<String, Value>` object scope), or a returned [Value] such as the
 * whole data object that this engine's `==` / `===` cannot distinguish — or
 * they exercise edge cases not present in the iOS conformance corpus.
 *
 * Everything else now lives in `predicate-fixtures/var.json`,
 * `missing.json`, and `missing_some.json`.
 */
class AccessorOperatorsTest {

    @get:Rule
    internal val loggerRule = CapturingLoggerRule()

    private val warnings: List<String> get() = loggerRule.warnings

    // ---- var ----

    @Test
    fun `var splits dot paths like json-logic-js preserving empty segments`() {
        // json-logic-js uses `String(path).split(".")`, which keeps empty
        // segments — e.g. `"a..b"` → `["a", "", "b"]`.
        val doubleDot = obj("a" to obj("" to obj("b" to s("middle"))))
        assertThat(AccessorOperators.opVar(s("a..b"), doubleDot)).isEqualTo(s("middle"))

        val leadingDot = obj("" to obj("a" to s("leading")))
        assertThat(AccessorOperators.opVar(s(".a"), leadingDot)).isEqualTo(s("leading"))

        val trailingDot = obj("a" to obj("" to s("trailing")))
        assertThat(AccessorOperators.opVar(s("a."), trailingDot)).isEqualTo(s("trailing"))

        val onlyDots = obj("" to obj("" to s("only-dots")))
        assertThat(AccessorOperators.opVar(s("."), onlyDots)).isEqualTo(s("only-dots"))
    }

    @Test
    fun `var default not used when leaf is null`() {
        // Default applies only when lookup fails. A present key whose value is
        // Null is returned as-is — json-logic-js distinguishes `undefined`
        // (missing) from an explicit null leaf.
        val vars = obj("key" to Value.Null)
        val out = AccessorOperators.opVar(
            Value.ArrayValue(listOf(s("key"), s("fallback"))),
            vars,
        )
        assertThat(out).isEqualTo(Value.Null)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var default used when mid-path breaks on null`() {
        // When descent hits a null parent, json-logic-js returns the default
        // rather than attempting further segments.
        val vars = obj("a" to Value.Null)
        val out = AccessorOperators.opVar(
            Value.ArrayValue(listOf(s("a.b"), s("fallback"))),
            vars,
        )
        assertThat(out).isEqualTo(s("fallback"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var empty path returns entire data`() {
        val vars = obj("x" to Value.IntValue(1))
        val out = AccessorOperators.opVar(s(""), vars)
        assertThat(out).isEqualTo(vars)
    }

    @Test
    fun `var null path returns entire data`() {
        // json-logic-js treats `undefined`, null, and "" as “return the
        // whole data object”.
        val vars = obj("x" to Value.IntValue(1))
        val out = AccessorOperators.opVar(Value.Null, vars)
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

    // ---- missing_some ----

    @Test
    fun `missing_some with non-numeric string threshold never satisfies`() {
        val result = runMissingSome(
            Value.ArrayValue(
                listOf(
                    s("abc"),
                    Value.ArrayValue(listOf(s("a"), s("b"))),
                ),
            ),
            obj(),
        )
        assertThat(result).isEqualTo(Value.ArrayValue(listOf(s("a"), s("b"))))
    }

    // ---- helpers ----

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)

    private fun runMissingSome(args: Value, vars: Value): Value =
        AccessorOperators.opMissingSome(args, vars)
}
