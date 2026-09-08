@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.LogMessage
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.assertDebugLog
import com.revenuecat.purchases.assertLogs
import com.revenuecat.purchases.assertVerboseLog
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.rules.RulesEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Date

class LocalRulesEvaluatorTest {

    private val matchingPredicate = """{"==": [{"var": "platform"}, "android"]}"""
    private val nonMatchingPredicate = """{"==": [{"var": "platform"}, "amazon"]}"""
    private val malformedPredicate = "{not json"
    private val unsuppliedDimensionPredicate = """{"==": [{"var": "unknown_dimension"}, true]}"""

    private var snapshotsTaken = 0
    private val deviceProvider = object : RulesDimensionProvider {
        override val name = "device"
        override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
            snapshotsTaken++
            return mapOf("platform" to RulesDimensionValue.StringValue("android"))
        }
    }

    @Before
    fun setup() {
        currentLogHandler = NoOpLogHandler
    }

    @Test
    fun `no rules matches nothing without collecting dimensions`() = runTest {
        val result = evaluator().match(emptyList<TestRule>())

        assertThat(result.getOrThrow()).isNull()
        assertThat(snapshotsTaken).isZero()
    }

    @Test
    fun `the first matching rule wins`() = runTest {
        val result = evaluator().match(
            listOf(
                TestRule("first", nonMatchingPredicate),
                TestRule("second", matchingPredicate),
                TestRule("third", matchingPredicate),
            ),
        )

        assertThat(result.getOrThrow()?.name).isEqualTo("second")
    }

    @Test
    fun `nothing matches when no predicate is satisfied`() = runTest {
        val result = evaluator().match(listOf(TestRule("only", nonMatchingPredicate)))

        assertThat(result.getOrThrow()).isNull()
    }

    @Test
    fun `a predicate reading an unsupplied dimension is a non-match`() = runTest {
        val result = evaluator().match(listOf(TestRule("only", unsuppliedDimensionPredicate)))

        assertThat(result.getOrThrow()).isNull()
    }

    @Test
    fun `a negated predicate reading an unsupplied dimension is a non-match`() = runTest {
        val result = evaluator().match(
            listOf(TestRule("only", """{"!": [{"==": [{"var": "unknown_dimension"}, "NL"]}]}""")),
        )

        assertThat(result.getOrThrow()).isNull()
    }

    @Test
    fun `an unsupplied dimension does not block a later match`() = runTest {
        val result = evaluator().match(
            listOf(
                TestRule("unsupplied", unsuppliedDimensionPredicate),
                TestRule("match", matchingPredicate),
            ),
        )

        assertThat(result.getOrThrow()?.name).isEqualTo("match")
    }

    @Test
    fun `an unsupplied dimension is not remembered as the first failure`() = runTest {
        val result = evaluator().match(
            listOf(
                TestRule("unsupplied", unsuppliedDimensionPredicate),
                TestRule("broken", malformedPredicate),
            ),
        )

        val error = result.exceptionOrNull() as LocalRulesEvaluationException.PredicateEvaluation
        assertThat(error.ruleIndex).isEqualTo(1)
        assertThat(error.error).isInstanceOf(RulesEngine.EvaluationException.Parse::class.java)
    }

    @Test
    fun `a later match wins over an earlier unevaluable predicate`() = runTest {
        val result = evaluator().match(
            listOf(
                TestRule("broken", malformedPredicate),
                TestRule("match", matchingPredicate),
            ),
        )

        assertThat(result.getOrThrow()?.name).isEqualTo("match")
    }

    @Test
    fun `an unevaluable predicate surfaces when nothing matched`() = runTest {
        val result = evaluator().match(
            listOf(
                TestRule("first", nonMatchingPredicate),
                TestRule("broken", malformedPredicate),
                TestRule("also broken", """{"nonexistent": []}"""),
            ),
        )

        val error = result.exceptionOrNull() as LocalRulesEvaluationException.PredicateEvaluation
        assertThat(error.ruleIndex).isEqualTo(1)
        assertThat(error.error).isInstanceOf(RulesEngine.EvaluationException.Parse::class.java)
    }

    @Test
    fun `a failed dimension snapshot fails the evaluation`() = runTest {
        val failing = object : RulesDimensionProvider {
            override val name = "device"
            override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
                throw IllegalStateException("nope")
        }

        val result = LocalRulesEvaluator(providers = listOf(failing), currentAppUserId = { "user" })
            .match(listOf(TestRule("only", matchingPredicate)))

        val error = result.exceptionOrNull() as LocalRulesEvaluationException.DimensionResolution
        assertThat(error.reason)
            .isEqualTo(RulesDimensionResolutionException.ProviderFailed("device", "nope"))
    }

    @Test
    fun `an app user change during the snapshot fails the evaluation`() = runTest {
        var currentUser = "userA"
        val flipping = object : RulesDimensionProvider {
            override val name = "identity_flipper"
            override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
                currentUser = "userB"
                return emptyMap()
            }
        }

        val result = LocalRulesEvaluator(providers = listOf(flipping), currentAppUserId = { currentUser })
            .match(listOf(TestRule("only", matchingPredicate)))

        val error = result.exceptionOrNull() as LocalRulesEvaluationException.DimensionResolution
        assertThat(error.reason).isInstanceOf(RulesDimensionResolutionException.AppUserChanged::class.java)
    }

    @Test
    fun `lazy predicates are resolved in order only until a rule matches`() = runTest {
        val resolved = mutableListOf<String>()
        val rules = listOf("first", "second", "unused")

        val result = evaluator().match(rules) { rule ->
            resolved += rule
            Result.success(if (rule == "second") matchingPredicate else nonMatchingPredicate)
        }

        assertThat(result.getOrThrow()).isEqualTo("second")
        assertThat(resolved).containsExactly("first", "second")
    }

    @Test
    fun `predicate resolution failure stops evaluation`() = runTest {
        val resolved = mutableListOf<String>()
        val failure = IllegalStateException("audience unavailable")

        val result = evaluator().match(listOf("missing", "unused")) { rule ->
            resolved += rule
            if (rule == "missing") Result.failure(failure) else Result.success(matchingPredicate)
        }

        assertThat(result.exceptionOrNull()).isSameAs(failure)
        assertThat(resolved).containsExactly("missing")
    }

    @Test
    fun `a predicate reading a custom variable matches`() = runTest {
        val rules = listOf(TestRule("only", """{"==": [{"var": "custom.source"}, "settings"]}"""))

        assertThat(
            evaluator().match(rules, mapOf("source" to RulesDimensionValue.StringValue("settings")))
                .getOrThrow()?.name,
        ).isEqualTo("only")
        assertThat(
            evaluator().match(rules, mapOf("source" to RulesDimensionValue.StringValue("other")))
                .getOrThrow(),
        ).isNull()
        // Supplying no custom variables at all leaves `custom.source` unresolved, which is a non-match.
        assertThat(evaluator().match(rules).getOrThrow()).isNull()
    }

    @Test
    fun `custom variables and ambient dimensions are visible in the same call`() = runTest {
        val rules = listOf(
            TestRule(
                "only",
                """{"and": [
                    {"==": [{"var": "platform"}, "android"]},
                    {"==": [{"var": "custom.source"}, "settings"]}
                ]}""",
            ),
        )

        val matched = evaluator()
            .match(rules, mapOf("source" to RulesDimensionValue.StringValue("settings")))

        assertThat(matched.getOrThrow()?.name).isEqualTo("only")
    }

    @Test
    fun `dimensions are collected once per call regardless of rule count`() = runTest {
        evaluator().match(listOf("first", "second", "third")) { rule ->
            Result.success(if (rule == "third") matchingPredicate else nonMatchingPredicate)
        }

        assertThat(snapshotsTaken).isEqualTo(1)
    }

    @Test
    fun `every rule's outcome is logged without predicates or values`() {
        assertLogs(
            listOf(
                LogMessage(LogLevel.VERBOSE, "Evaluating 2 rules against dimensions [evaluated_at, platform]."),
                LogMessage(LogLevel.VERBOSE, "Rule 1 did not match."),
                LogMessage(LogLevel.VERBOSE, "Rule 2 matched."),
            ),
        ) {
            runTest {
                evaluator().match(
                    listOf(TestRule("first", nonMatchingPredicate), TestRule("second", matchingPredicate)),
                )
            }
        }
    }

    @Test
    fun `an unsupplied dimension is logged by name`() {
        assertVerboseLog("Rule 1 did not match: it reads 'unknown_dimension', which this SDK does not supply.") {
            runTest { evaluator().match(listOf(TestRule("only", unsuppliedDimensionPredicate))) }
        }
    }

    @Test
    fun `an unevaluable predicate is logged by failure kind`() {
        assertDebugLog("Rule 1 could not be evaluated (Parse).") {
            runTest { evaluator().match(listOf(TestRule("only", malformedPredicate))) }
        }
    }

    private fun evaluator() = LocalRulesEvaluator(providers = listOf(deviceProvider), currentAppUserId = { "user" })

    private data class TestRule(
        val name: String,
        override val predicate: String,
    ) : LocalRule
}
