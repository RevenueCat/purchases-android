package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLoggerRule
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

/**
 * The cases below are kept as Kotlin tests because they cannot be expressed
 * as JSON predicate fixtures (which evaluate a predicate to a boolean):
 *  - they use a top-level *array* scope, while `Evaluator.evaluate` always
 *    takes a `Map<String, Value>` object scope; or
 *  - they assert a returned [Value] (the whole data object) that this
 *    engine's `==` / `===` cannot distinguish.
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
    fun `var empty path returns entire data`() {
        val data = obj("x" to Value.IntValue(1))
        val out = AccessorOperators.opVar(s(""), Scope(root = data))
        assertThat(out).isEqualTo(data)
    }

    @Test
    fun `var null path returns entire data`() {
        // json-logic-js treats `undefined`, null, and "" as “return the
        // whole data object”.
        val data = obj("x" to Value.IntValue(1))
        val out = AccessorOperators.opVar(Value.Null, Scope(root = data))
        assertThat(out).isEqualTo(data)
    }

    @Test
    fun `var with numeric path arg is coerced to string`() {
        // {"var": 0} on array data
        val data = Value.ArrayValue(listOf(s("zero"), s("one")))
        val out = AccessorOperators.opVar(Value.IntValue(0), Scope(root = data))
        assertThat(out).isEqualTo(s("zero"))
    }

    @Test
    fun `var with integer-valued float path looks up integer index`() {
        // {"var": 1.0} on array data must render as "1" (not "1.0") so the
        // path resolves to array index 1 — same lookup as `{"var": 1}`.
        val data = Value.ArrayValue(listOf(s("zero"), s("one"), s("two")))
        val out = AccessorOperators.opVar(Value.FloatValue(1.0), Scope(root = data))
        assertThat(out).isEqualTo(s("one"))
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `var with fractional float path does not match adjacent indices`() {
        // {"var": 1.5} must not silently collapse to "1" or "2" — its
        // rendered path is "1.5", which doesn't resolve. Guards against an
        // over-eager rounding fix to `formatNumber`.
        val data = Value.ArrayValue(listOf(s("zero"), s("one"), s("two")))
        assertThatThrownBy { AccessorOperators.opVar(Value.FloatValue(1.5), Scope(root = data)) }
            .isEqualTo(RulesEngine.EvaluationException.UnresolvedVariable("1.5"))
    }

    // ---- helpers ----

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
