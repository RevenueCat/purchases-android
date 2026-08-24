package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RulesEngineEvaluateTest {

    @Test
    fun `evaluates truthy predicate`() {
        val result = RulesEngine.evaluate("true", emptyMap())
        assertThat(result.getOrNull()).isEqualTo(true)
    }

    @Test
    fun `evaluates falsy predicate`() {
        val result = RulesEngine.evaluate("false", emptyMap())
        assertThat(result.getOrNull()).isEqualTo(false)
    }

    @Test
    fun `evaluates predicate against variables`() {
        val result = RulesEngine.evaluate(
            """{"==":[{"var":"x"},1]}""",
            mapOf("x" to Value.IntValue(1)),
        )
        assertThat(result.getOrNull()).isEqualTo(true)
    }

    @Test
    fun `malformed JSON yields parse failure`() {
        val result = RulesEngine.evaluate("{not json", emptyMap())
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationException.Parse::class.java)
    }

    @Test
    fun `unsupported operator yields failure`() {
        val result = RulesEngine.evaluate("""{"nope":[]}""", emptyMap())
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationException.UnsupportedOperator::class.java)
    }

    @Test
    fun `transform returns the predicate result`() {
        val result = RulesEngine.transform(
            """{"var":"nested"}""",
            mapOf("nested" to Value.ObjectValue(mapOf("x" to Value.IntValue(1)))),
        )
        assertThat(result.getOrNull()).isEqualTo(mapOf("x" to Value.IntValue(1)))
    }

    @Test
    fun `transform with an identity predicate returns variables unchanged`() {
        val variables = mapOf("x" to Value.IntValue(1))
        val result = RulesEngine.transform("""{"var":""}""", variables)
        assertThat(result.getOrNull()).isEqualTo(variables)
    }

    @Test
    fun `transform and evaluate use independent predicates`() {
        val raw = mapOf("x" to Value.IntValue(1))
        val transformed = RulesEngine.transform("""{"var":""}""", raw).getOrThrow()
        val result = RulesEngine.evaluate(
            """{"==":[{"var":"x"},1]}""",
            transformed,
        )
        assertThat(result.getOrNull()).isEqualTo(true)
    }

    @Test
    fun `transform to a non-object yields type mismatch failure`() {
        val result = RulesEngine.transform("""{"var":"x"}""", mapOf("x" to Value.IntValue(1)))
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationException.TypeMismatch::class.java)
    }

    @Test
    fun `transform fails when a condition reads an absent variable`() {
        // The obvious way to write an optional scope is to use the variable as its own condition.
        // An absent name is not a falsy one, so this is unanswerable rather than a fallback.
        val result = RulesEngine.transform(
            """{"if":[{"var":"optionalScope"},{"var":"optionalScope"},{"var":""}]}""",
            emptyMap(),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationException.UnresolvedVariable::class.java)
    }

    @Test
    fun `malformed JSON yields parse failure in transform`() {
        val result = RulesEngine.transform("{not json", emptyMap())
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationException.Parse::class.java)
    }
}
