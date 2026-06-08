package com.revenuecat.purchases.rules.helpers

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

/**
 * Runs a single [PredicateConformanceFixtureCase]: evaluates the predicate,
 * asserts the expected outcome, and (when declared) asserts the emitted
 * warnings. [warnings] is read lazily so it reflects what the evaluation
 * produced.
 */
internal object PredicateConformanceRunner {

    /**
     * Test-only numeric constants seeded into every fixture's variable
     * scope so fixtures can reference values JSON cannot express as a
     * literal (`±Infinity`). These exist ONLY in the test harness — they
     * are never part of the production engine or real rule data.
     */
    private val reservedConstants: Map<String, Value> = mapOf(
        "+Infinity" to Value.FloatValue(Double.POSITIVE_INFINITY),
        "-Infinity" to Value.FloatValue(Double.NEGATIVE_INFINITY),
    )

    /** Fixture-declared variables take precedence over the reserved constants on a name collision. */
    private fun scope(fixture: PredicateConformanceFixtureCase): Map<String, Value> =
        reservedConstants + fixture.variables

    fun run(
        fixture: PredicateConformanceFixtureCase,
        warnings: () -> List<String>,
        logs: () -> List<String>,
    ) {
        assertExpectedOutcome(fixture)
        fixture.expectedWarnings?.let { assertWarnings(warnings(), it, fixture.id) }
        fixture.expectedLogs?.let { assertLogs(logs(), it, fixture.id) }
    }

    private fun assertExpectedOutcome(fixture: PredicateConformanceFixtureCase) {
        when (val expected = fixture.expected) {
            is ExpectedOutcome.BooleanOutcome -> {
                val result = Evaluator.evaluate(fixture.predicate, scope(fixture))
                assertThat(result)
                    .withFailMessage("Fixture %s: expected %s but got %s", fixture.id, expected.value, result)
                    .isEqualTo(expected.value)
            }
            is ExpectedOutcome.ErrorOutcome -> assertErrorOutcome(fixture, expected.error)
        }
    }

    private fun assertErrorOutcome(fixture: PredicateConformanceFixtureCase, expected: ExpectedError) {
        assertThatThrownBy { Evaluator.evaluate(fixture.predicate, scope(fixture)) }
            .withFailMessage("Fixture %s: expected error %s but did not throw", fixture.id, expected.kind)
            .isInstanceOfSatisfying(RuleError::class.java) { error ->
                assertThat(matchesExpected(error, expected))
                    .withFailMessage("Fixture %s: threw %s, expected %s", fixture.id, error, describe(expected))
                    .isTrue()
            }
    }

    private fun matchesExpected(error: RuleError, expected: ExpectedError): Boolean = when (expected.kind) {
        "typeMismatch" -> error is RuleError.TypeMismatch
        "unsupportedOperator" -> error is RuleError.UnsupportedOperator &&
            (expected.operator == null || error.name == expected.operator)
        else -> false
    }

    private fun describe(expected: ExpectedError): String =
        expected.operator?.let { "${expected.kind}($it)" } ?: expected.kind

    private fun assertWarnings(warnings: List<String>, expected: ExpectedWarnings, id: String) {
        if (expected.contains.isEmpty()) {
            assertThat(warnings)
                .withFailMessage("Fixture %s: expected no warnings, got %s", id, warnings)
                .isEmpty()
            return
        }
        expected.contains.forEach { substring ->
            assertThat(warnings)
                .withFailMessage("Fixture %s: missing warning containing \"%s\", got %s", id, substring, warnings)
                .anyMatch { it.contains(substring) }
        }
    }

    private fun assertLogs(logs: List<String>, expected: List<String>, id: String) {
        assertThat(logs)
            .withFailMessage("Fixture %s: expected logs %s, got %s", id, expected, logs)
            .isEqualTo(expected)
    }
}
