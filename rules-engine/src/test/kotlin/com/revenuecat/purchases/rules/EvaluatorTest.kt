package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.ValueJsonHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

@Suppress("LargeClass")
class EvaluatorTest {

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
        val logger = CapturingLogger()
        val result = Evaluator.evaluate(
            ValueJsonHelper.fromJsonString(predicate),
            emptyMap(),
            logger,
        )
        assertThat(result).isTrue
        assertThat(logger.warnings).hasSize(1)
        assertThat(logger.warnings[0]).contains("missing")
    }

    // ---- error paths ----

    @Test
    fun `unsupported operator surfaces error`() {
        val predicate = ValueJsonHelper.fromJsonString("""{"someUnknownOp": [1, 2]}""")
        assertThatThrownBy {
            Evaluator.evaluate(predicate, emptyMap(), CapturingLogger())
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
    fun `arity error on binary operator surfaces type mismatch`() {
        val predicate = ValueJsonHelper.fromJsonString("""{"==": [1]}""")
        assertThatThrownBy {
            Evaluator.evaluate(predicate, emptyMap(), CapturingLogger())
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- multi-key object treated as data, not operator ----

    @Test
    fun `multi-key object is a literal data value`() {
        // An object literal with two keys isn't an operator.
        val predicateEq = """
            {"==": [
                {"a": 1, "b": 2},
                {"a": 1, "b": 2}
            ]}
        """.trimIndent()
        assertThat(run(predicateEq)).isTrue

        // Same two literals through `!=` should evaluate to false (they are
        // equal as data, so the inequality is unsatisfied). Confirms the
        // literal-vs-operator handling is symmetric across operators.
        val predicateNe = """
            {"!=": [
                {"a": 1, "b": 2},
                {"a": 1, "b": 2}
            ]}
        """.trimIndent()
        assertThat(run(predicateNe)).isFalse
    }

    // ---- helpers ----

    private fun run(predicateJson: String, vars: Map<String, Value> = emptyMap()): Boolean {
        val predicate = ValueJsonHelper.fromJsonString(predicateJson)
        return Evaluator.evaluate(predicate, vars, CapturingLogger())
    }

    private fun obj(vararg entries: Pair<String, Value>): Value =
        Value.ObjectValue(entries.toMap())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
