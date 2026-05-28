package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.ValueJsonHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

@Suppress("LargeClass")
class EvaluatorTest {

    @get:Rule
    internal val loggerRule = CapturingLoggerRule()

    // ---- literal predicates ----

    @Test
    fun `literal true is truthy`() {
        assertThat(run("true")).isTrue
        assertThat(run("1")).isTrue
        assertThat(run("\"hello\"")).isTrue
    }

    @Test
    fun `literal false is falsy`() {
        assertThat(run("false")).isFalse
        assertThat(run("0")).isFalse
        assertThat(run("\"\"")).isFalse
        assertThat(run("null")).isFalse
    }

    // ---- composite predicate integration ----

    @Test
    fun `composite predicate evaluates correctly`() {
        val predicate = """
            {
                "and": [
                    {"==": [{"var": "subscriber.last_seen_country"}, "US"]},
                    {"==": [{"var": "session.app_launch_count"}, 3]}
                ]
            }
        """.trimIndent()
        val vars = mapOf<String, Value>(
            "subscriber" to obj("last_seen_country" to s("US")),
            "session" to obj("app_launch_count" to Value.IntValue(3)),
        )
        assertThat(run(predicate, vars)).isTrue
    }

    @Test
    fun `composite predicate evaluates to false when country differs`() {
        val predicate = """
            {
                "and": [
                    {"==": [{"var": "subscriber.last_seen_country"}, "US"]},
                    {"==": [{"var": "session.app_launch_count"}, 3]}
                ]
            }
        """.trimIndent()
        val vars = mapOf<String, Value>(
            "subscriber" to obj("last_seen_country" to s("CA")),
            "session" to obj("app_launch_count" to Value.IntValue(3)),
        )
        assertThat(run(predicate, vars)).isFalse
    }

    // ---- nested expressions ----

    @Test
    fun `nested or within and`() {
        // (country in {"US","CA"}) AND (count == 3)
        // Without an `in` operator yet, model as:
        // (country == "US" OR country == "CA") AND ...
        val predicate = """
            {
                "and": [
                    {"or": [
                        {"==": [{"var": "country"}, "US"]},
                        {"==": [{"var": "country"}, "CA"]}
                    ]},
                    {"==": [{"var": "count"}, 3]}
                ]
            }
        """.trimIndent()
        assertThat(run(predicate, mapOf("country" to s("CA"), "count" to Value.IntValue(3)))).isTrue
        assertThat(run(predicate, mapOf("country" to s("MX"), "count" to Value.IntValue(3)))).isFalse
    }

    @Test
    fun `if chooses branch based on var`() {
        // `true` / `false` literal branches so the assertion can distinguish
        // which branch was taken (string branches would both be truthy).
        val predicate = """
            {
                "if": [
                    {"==": [{"var": "tier"}, "premium"]},
                    true,
                    false
                ]
            }
        """.trimIndent()
        assertThat(run(predicate, mapOf("tier" to s("premium")))).isTrue
        assertThat(run(predicate, mapOf("tier" to s("free")))).isFalse
    }

    @Test
    fun `and returns last truthy value not boolean so order matters`() {
        // `{"and": [a, b, c]}` returns the last truthy value, not a coerced
        // bool. A regression that started bool-coercing would make the
        // strict-eq comparison flip and we'd catch it here.
        val predicate = """
            {"===": [
                {"and": ["premium", 5, true]},
                {"and": [true, 5, "premium"]}
            ]}
        """.trimIndent()
        // Left AND returns `true` (last); right AND returns `"premium"`.
        // `true === "premium"` is false.
        assertThat(run(predicate)).isFalse
    }

    @Test
    fun `if branch value flows into outer predicate`() {
        // The inner `if` returns the string "premium" or "free", which the
        // outer `==` compares. If the engine were coercing the `if` result
        // to a bool too eagerly, both branches would equal `true` and the
        // comparison would never distinguish them.
        val predicate = """
            {
                "and": [
                    {"==": [{"var": "active"}, true]},
                    {"==": [
                        {"if": [{"var": "is_paid"}, "premium", "free"]},
                        "premium"
                    ]}
                ]
            }
        """.trimIndent()
        assertThat(
            run(
                predicate,
                mapOf("active" to Value.BoolValue(true), "is_paid" to Value.BoolValue(true)),
            ),
        ).isTrue
        assertThat(
            run(
                predicate,
                mapOf("active" to Value.BoolValue(true), "is_paid" to Value.BoolValue(false)),
            ),
        ).isFalse
    }

    // ---- missing-variable behavior ----

    @Test
    fun `missing variable resolves to null and warns`() {
        // {"==": [{"var": "missing"}, null]} should be true when the var is
        // missing, since missing → null and null == null.
        val predicate = """{"==": [{"var": "missing"}, null]}"""
        val result = Evaluator.evaluate(
            ValueJsonHelper.fromJsonString(predicate),
            emptyMap(),
        )
        assertThat(result).isTrue
        assertThat(loggerRule.warnings).hasSize(1)
        assertThat(loggerRule.warnings[0]).contains("missing")
    }

    // ---- error paths ----

    @Test
    fun `unsupported operator surfaces error`() {
        val predicate = ValueJsonHelper.fromJsonString("""{"someUnknownOp": [1, 2]}""")
        assertThatThrownBy {
            Evaluator.evaluate(predicate, emptyMap())
        }.isInstanceOf(RuleError.UnsupportedOperator::class.java)
    }

    @Test
    fun `malformed JSON surfaces parse error`() {
        // Parse errors now surface from the test-only JSON helper (production
        // callers parse on the native side and never hand `evaluate` a
        // malformed tree). The error type is still `RuleError.Parse`.
        assertThatThrownBy {
            ValueJsonHelper.fromJsonString("{not json")
        }.isInstanceOf(RuleError.Parse::class.java)
    }

    @Test
    fun `binary operator missing operand compares against null`() {
        // `json-logic-js` declares binary operators (`==`, `===`, `!=`,
        // `!==`, `in`, etc.) as `function(a, b)`, so a missing second
        // operand stands in for JS `undefined`. The loose-equality path
        // then matches our `null` ↔ `undefined` behavior and returns
        // `false` for `1 == undefined`.
        val predicate = ValueJsonHelper.fromJsonString("""{"==": [1]}""")
        val result = Evaluator.evaluate(predicate, emptyMap())
        assertThat(result).isFalse
    }

    // ---- arithmetic dispatched through evaluator ----

    @Test
    fun `arithmetic predicate with var operand`() {
        // session.app_launch_count * 2 == 6 → true when count is 3
        val predicate = """
            {"==": [
                {"*": [{"var": "session.app_launch_count"}, 2]},
                6
            ]}
        """.trimIndent()
        val vars = mapOf<String, Value>(
            "session" to obj("app_launch_count" to Value.IntValue(3)),
        )
        assertThat(run(predicate, vars)).isTrue
    }

    @Test
    fun `divide by zero produces IEEE 754 values that flow through truthiness`() {
        // `n / 0` follows IEEE 754 (matches json-logic-js, no short-circuit).
        // {"/": [10, 0]} → +Infinity → truthy.
        assertThat(run("""{"/": [10, 0]}""")).isTrue
        // {"/": [0, 0]} → NaN → falsy (NaN is the one float that isTruthy
        // reports as false).
        assertThat(run("""{"/": [0, 0]}""")).isFalse
    }

    // ---- multi-key object treated as data, not operator ----

    @Test
    fun `multi-key object is literal data value`() {
        // Mirrors json-logic-js's `is_logic`, which only treats an object
        // as an operator when `Object.keys(logic).length === 1`. A two-key
        // object falls back to `apply`'s "not logic, return as-is" branch
        // and reaches `==` as a literal data value. JS abstract equality
        // then uses reference identity for the two objects → `false`.
        val predicateEq = """
            {"==": [
                {"a": 1, "b": 2},
                {"a": 1, "b": 2}
            ]}
        """.trimIndent()
        assertThat(run(predicateEq)).isFalse

        // Symmetric `!=`: distinct object references are unequal, so
        // the inequality holds.
        val predicateNe = """
            {"!=": [
                {"a": 1, "b": 2},
                {"a": 1, "b": 2}
            ]}
        """.trimIndent()
        assertThat(run(predicateNe)).isTrue
    }

    // ---- equality with JS-style array/object coercion ----

    @Test
    fun `loose equality coerces array to JS string end-to-end`() {
        // Pins the spec-aligned coercion path (Array.prototype.toString)
        // through the full evaluator, not just the looseEq helper:
        // `{"==": [[1, 2], "1,2"]}` → true, mirroring json-logic-js.
        assertThat(run("""{"==": [[1, 2], "1,2"]}""")).isTrue
        // Numeric fallback after ToPrimitive: `[1] == 1`.
        assertThat(run("""{"==": [[1], 1]}""")).isTrue
        // Empty array stringifies to "" which numerically coerces to 0.
        assertThat(run("""{"==": [[], 0]}""")).isTrue
    }

    @Test
    fun `loose equality coerces object to JS string end-to-end`() {
        // A multi-key object (so it isn't dispatched as an operator)
        // coerces to "[object Object]" against a string operand. Pins
        // the rare-but-real case where a payload field gets accidentally
        // serialized through `String(value)` upstream.
        val predicate = """
            {"==": [
                {"a": 1, "b": 2},
                "[object Object]"
            ]}
        """.trimIndent()
        assertThat(run(predicate)).isTrue
    }

    @Test
    fun `single-key object operand is dispatched as operator`() {
        // Pins the contrast with the multi-key case above: single-key
        // objects flow through `Evaluator.evaluateValue` like any other
        // expression and get dispatched as operators (the `is_logic` →
        // `apply` path in json-logic-js). An unknown op name surfaces as
        // `RuleError.UnsupportedOperator`, mirroring the JS reference's
        // `Unrecognized operation a` throw — so even though the multi-key
        // case `{a:1,b:2} == {a:1,b:2}` returns `true` in our engine and
        // `false` in JS (deliberate structural-vs-reference divergence),
        // the literal `{a:1} == {a:1}` does NOT diverge: both engines
        // fail to evaluate it.
        val predicate = ValueJsonHelper.fromJsonString("""{"==": [{"a": 1}, {"a": 1}]}""")
        assertThatThrownBy {
            Evaluator.evaluate(predicate, emptyMap())
        }
            .isInstanceOfSatisfying(RuleError.UnsupportedOperator::class.java) { error ->
                assertThat(error.name).isEqualTo("a")
            }
    }

    // ---- literal predicate truthiness ----

    @Test
    fun `literal empty array predicate is falsy`() {
        assertThat(run("[]")).isFalse
    }

    @Test
    fun `literal non-empty array predicate is truthy even with falsy elements`() {
        // Per http://jsonlogic.com/truthy — non-empty arrays are truthy
        // regardless of element values.
        assertThat(run("[false]")).isTrue
        assertThat(run("[0]")).isTrue
    }

    @Test
    fun `literal object predicate is truthy even with falsy values`() {
        // Multi-key objects are literal data (not operator dispatch) and
        // objects are always truthy in JSON Logic.
        assertThat(run("""{"a": false, "b": 0}""")).isTrue
    }

    // ---- helpers ----

    private fun run(predicateJson: String, vars: Map<String, Value> = emptyMap()): Boolean {
        val predicate = ValueJsonHelper.fromJsonString(predicateJson)
        return Evaluator.evaluate(predicate, vars)
    }

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
