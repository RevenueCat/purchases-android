package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.ValueJsonHelper
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class EvaluatorTest {

    // Kept as a Kotlin test (not migrated to a JSON fixture): this asserts
    // that parsing a malformed JSON *string* throws. A fixture's `predicate`
    // must itself be valid JSON in the file, so malformed input can only be
    // exercised through the test-only `ValueJsonHelper.fromJsonString` helper.
    // Every other evaluator behavior now lives in
    // `predicate-fixtures/evaluator.json`.
    @Test
    fun `malformed JSON surfaces parse error`() {
        assertThatThrownBy {
            ValueJsonHelper.fromJsonString("{not json")
        }.isInstanceOf(RuleError.Parse::class.java)
    }
}
