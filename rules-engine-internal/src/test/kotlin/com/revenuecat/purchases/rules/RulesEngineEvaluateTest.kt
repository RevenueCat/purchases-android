package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.RulesEngine.EvaluationError
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
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationError.Parse::class.java)
    }

    @Test
    fun `unsupported operator yields failure`() {
        val result = RulesEngine.evaluate("""{"nope":[]}""", emptyMap())
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EvaluationError.UnsupportedOperator::class.java)
    }
}
