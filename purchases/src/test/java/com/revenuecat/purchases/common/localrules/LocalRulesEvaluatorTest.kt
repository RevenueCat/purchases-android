@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.rules.RulesEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class LocalRulesEvaluatorTest {

    private val matchingPredicate = """{"==": [{"var": "device.platform"}, "android"]}"""
    private val nonMatchingPredicate = """{"==": [{"var": "device.platform"}, "amazon"]}"""
    private val malformedPredicate = "{not json"

    private var snapshotsTaken = 0
    private val deviceProvider = object : RulesDimensionProvider {
        override val identifier = "device"
        override val namespace = RulesDimensionNamespace.Device
        override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
            snapshotsTaken++
            return mapOf("platform" to RulesDimensionValue.StringValue("android"))
        }
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
    fun `a predicate reading an unsupplied dimension is an ordinary non-match`() = runTest {
        val result = evaluator().match(
            listOf(TestRule("only", """{"==": [{"var": "device.unknown_dimension"}, true]}""")),
        )

        assertThat(result.getOrThrow()).isNull()
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
            override val identifier = "failing"
            override val namespace = RulesDimensionNamespace.Device
            override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
                throw IllegalStateException("nope")
        }

        val result = LocalRulesEvaluator(providers = listOf(failing))
            .match(listOf(TestRule("only", matchingPredicate)))

        val error = result.exceptionOrNull() as LocalRulesEvaluationException.DimensionResolution
        assertThat(error.reason)
            .isEqualTo(RulesDimensionResolutionException.ProviderFailed("failing", "nope"))
    }

    @Test
    fun `dimensions are collected once per call regardless of rule count`() = runTest {
        evaluator().match(
            listOf(
                TestRule("first", nonMatchingPredicate),
                TestRule("second", nonMatchingPredicate),
                TestRule("third", matchingPredicate),
            ),
        )

        assertThat(snapshotsTaken).isEqualTo(1)
    }

    private fun evaluator() = LocalRulesEvaluator(providers = listOf(deviceProvider))

    private data class TestRule(
        val name: String,
        override val predicate: String,
    ) : LocalRule
}
