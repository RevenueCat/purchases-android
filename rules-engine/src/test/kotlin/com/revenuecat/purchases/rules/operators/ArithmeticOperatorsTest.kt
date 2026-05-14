package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.PrintlnLogger
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class ArithmeticOperatorsTest {

    // ---- + ----

    @Test
    fun `add sums two ints`() {
        assertThat(run(ArithmeticOperators::opAdd, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.FloatValue(3.0))
    }

    @Test
    fun `add is variadic`() {
        assertThat(
            run(
                ArithmeticOperators::opAdd,
                arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3), Value.IntValue(4)),
            ),
        ).isEqualTo(Value.FloatValue(10.0))
    }

    @Test
    fun `add one arg acts as numeric cast`() {
        // {"+": ["2.5"]} → 2.5
        assertThat(run(ArithmeticOperators::opAdd, arr(s("2.5"))))
            .isEqualTo(Value.FloatValue(2.5))
        // {"+": [true]} → 1
        assertThat(run(ArithmeticOperators::opAdd, arr(Value.BoolValue(true))))
            .isEqualTo(Value.FloatValue(1.0))
    }

    @Test
    fun `add coerces strings and bools`() {
        // "1" + 1 → 2
        assertThat(run(ArithmeticOperators::opAdd, arr(s("1"), Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(2.0))
        // true + 1 + 1 → 3
        assertThat(
            run(
                ArithmeticOperators::opAdd,
                arr(Value.BoolValue(true), Value.IntValue(1), Value.IntValue(1)),
            ),
        ).isEqualTo(Value.FloatValue(3.0))
    }

    @Test
    fun `add non-numeric propagates NaN`() {
        // Object + 1 → NaN (object can't coerce, propagates)
        val result = run(
            ArithmeticOperators::opAdd,
            arr(Value.ObjectValue(emptyMap()), Value.IntValue(1)),
        )
        assertThat((result as Value.FloatValue).value.isNaN()).isTrue
    }

    @Test
    fun `add zero args is type error`() {
        assertThatThrownBy { run(ArithmeticOperators::opAdd, arr()) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- * ----

    @Test
    fun `mul multiplies args`() {
        assertThat(
            run(
                ArithmeticOperators::opMul,
                arr(Value.IntValue(2), Value.IntValue(3), Value.IntValue(4)),
            ),
        ).isEqualTo(Value.FloatValue(24.0))
    }

    @Test
    fun `mul one arg returns value as float`() {
        assertThat(run(ArithmeticOperators::opMul, arr(Value.IntValue(5))))
            .isEqualTo(Value.FloatValue(5.0))
    }

    @Test
    fun `mul coerces operands`() {
        // "2" * "3" → 6
        assertThat(run(ArithmeticOperators::opMul, arr(s("2"), s("3"))))
            .isEqualTo(Value.FloatValue(6.0))
    }

    @Test
    fun `mul zero args is type error`() {
        assertThatThrownBy { run(ArithmeticOperators::opMul, arr()) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- - ----

    @Test
    fun `sub unary negates`() {
        // {"-": [3]} → -3
        assertThat(run(ArithmeticOperators::opSub, arr(Value.IntValue(3))))
            .isEqualTo(Value.FloatValue(-3.0))
        // Negating a string-encoded number still works
        assertThat(run(ArithmeticOperators::opSub, arr(s("2.5"))))
            .isEqualTo(Value.FloatValue(-2.5))
    }

    @Test
    fun `sub binary subtracts`() {
        assertThat(run(ArithmeticOperators::opSub, arr(Value.IntValue(10), Value.IntValue(3))))
            .isEqualTo(Value.FloatValue(7.0))
    }

    @Test
    fun `sub three or more args is type error`() {
        assertThatThrownBy {
            run(
                ArithmeticOperators::opSub,
                arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3)),
            )
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    @Test
    fun `sub zero args is type error`() {
        assertThatThrownBy { run(ArithmeticOperators::opSub, arr()) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- / ----

    @Test
    fun `div basic`() {
        assertThat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(10), Value.IntValue(2))))
            .isEqualTo(Value.FloatValue(5.0))
    }

    @Test
    fun `div coerces operands`() {
        assertThat(run(ArithmeticOperators::opDiv, arr(s("9"), s("3"))))
            .isEqualTo(Value.FloatValue(3.0))
    }

    @Test
    fun `div by zero returns null`() {
        // Both IntValue(0) and FloatValue(0.0) divisors → Null
        assertThat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(1), Value.IntValue(0))))
            .isEqualTo(Value.Null)
        assertThat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(1), Value.FloatValue(0.0))))
            .isEqualTo(Value.Null)
        // Even 0/0 returns Null (not NaN) — explicit short-circuit before
        // arithmetic.
        assertThat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(0), Value.IntValue(0))))
            .isEqualTo(Value.Null)
    }

    @Test
    fun `div wrong arity is type error`() {
        assertThatThrownBy { run(ArithmeticOperators::opDiv, arr(Value.IntValue(1))) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- % ----

    @Test
    fun `mod basic`() {
        assertThat(run(ArithmeticOperators::opMod, arr(Value.IntValue(10), Value.IntValue(3))))
            .isEqualTo(Value.FloatValue(1.0))
    }

    @Test
    fun `mod by zero returns null`() {
        assertThat(run(ArithmeticOperators::opMod, arr(Value.IntValue(7), Value.IntValue(0))))
            .isEqualTo(Value.Null)
    }

    @Test
    fun `mod wrong arity is type error`() {
        assertThatThrownBy {
            run(
                ArithmeticOperators::opMod,
                arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3)),
            )
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- helpers ----

    private fun run(
        op: (Value, Value, com.revenuecat.purchases.rules.RulesEngineLogger) -> Value,
        args: Value,
    ): Value = op(args, Value.Null, PrintlnLogger)

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
