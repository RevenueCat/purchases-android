package com.revenuecat.purchases.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class RulesEngineTest {

    @Test
    fun `evaluate returns true for a truthy predicate`() {
        assertThat(RulesEngine.evaluate("true", emptyMap()).getOrThrow()).isTrue()
    }

    @Test
    fun `evaluate returns false for a falsy predicate`() {
        assertThat(RulesEngine.evaluate("false", emptyMap()).getOrThrow()).isFalse()
    }

    @Test
    fun `evaluate uses the provided variables`() {
        val result = RulesEngine.evaluate(
            """{"==":[{"var":"x"},1]}""",
            mapOf("x" to Value.IntValue(1)),
        )
        assertThat(result.getOrThrow()).isTrue()
    }

    @Test
    fun `evaluate returns parse failure for malformed JSON`() {
        val result = RulesEngine.evaluate("{not json", emptyMap())
        assertThat(result.exceptionOrNull()).isInstanceOf(RulesEngine.EvaluationException.Parse::class.java)
    }

    @Test
    fun `transform returns the predicate result`() {
        val result = RulesEngine.transform(
            """{"var":"nested"}""",
            mapOf("nested" to Value.ObjectValue(mapOf("x" to Value.IntValue(1)))),
        )
        assertThat(result.getOrThrow()).isEqualTo(mapOf("x" to Value.IntValue(1)))
    }

    @Test
    fun `transform with an identity predicate returns variables unchanged`() {
        val variables = mapOf("x" to Value.IntValue(1))
        val result = RulesEngine.transform("""{"var":""}""", variables)
        assertThat(result.getOrThrow()).isEqualTo(variables)
    }

    @Test
    fun `transform returns type mismatch failure when the result is not an object`() {
        val result = RulesEngine.transform("""{"var":"x"}""", mapOf("x" to Value.IntValue(1)))
        assertThat(result.exceptionOrNull()).isInstanceOf(RulesEngine.EvaluationException.TypeMismatch::class.java)
    }

    @Test
    fun `transform and evaluate use independent predicates`() {
        val raw = mapOf("x" to Value.IntValue(1))
        val transformed = RulesEngine.transform("""{"var":""}""", raw).getOrThrow()
        val result = RulesEngine.evaluate(
            """{"==":[{"var":"x"},1]}""",
            transformed,
        )
        assertThat(result.getOrThrow()).isTrue()
    }

    @Test
    fun `transform returns parse failure for malformed JSON`() {
        val result = RulesEngine.transform("{not json", emptyMap())
        assertThat(result.exceptionOrNull()).isInstanceOf(RulesEngine.EvaluationException.Parse::class.java)
    }
}
