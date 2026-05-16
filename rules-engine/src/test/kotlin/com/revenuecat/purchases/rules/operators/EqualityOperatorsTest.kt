package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLoggerRule
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

class EqualityOperatorsTest {

    @get:Rule
    internal val loggerRule = CapturingLoggerRule()

    @Test
    fun `looseEq basic`() {
        assertThat(evalEq(arr(Value.IntValue(1), Value.IntValue(1)))).isEqualTo(Value.BoolValue(true))
        assertThat(evalEq(arr(Value.IntValue(1), Value.IntValue(2)))).isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `looseEq does type coercion`() {
        // "1" == 1
        assertThat(evalEq(arr(Value.StringValue("1"), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
        // true == 1
        assertThat(evalEq(arr(Value.BoolValue(true), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `strictEq does not coerce`() {
        // "1" !== 1
        assertThat(evalStrictEq(arr(Value.StringValue("1"), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(false))
        // 1 === 1
        assertThat(evalStrictEq(arr(Value.IntValue(1), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
        // 1 === 1.0 (int/float bridge as one number type)
        assertThat(evalStrictEq(arr(Value.IntValue(1), Value.FloatValue(1.0))))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `looseNe is negation of looseEq`() {
        assertThat(evalNe(arr(Value.IntValue(1), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(evalNe(arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `strictNe is negation of strictEq`() {
        assertThat(evalStrictNe(arr(Value.IntValue(1), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(evalStrictNe(arr(Value.StringValue("1"), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `arity mismatch is type error`() {
        assertThatThrownBy {
            EqualityOperators.opLooseEq(arr(Value.IntValue(1)), Value.Null)
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    private fun evalEq(args: Value): Value = EqualityOperators.opLooseEq(args, Value.Null)
    private fun evalNe(args: Value): Value = EqualityOperators.opLooseNe(args, Value.Null)
    private fun evalStrictEq(args: Value): Value = EqualityOperators.opStrictEq(args, Value.Null)
    private fun evalStrictNe(args: Value): Value = EqualityOperators.opStrictNe(args, Value.Null)

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())
}
