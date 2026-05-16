package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.PrintlnLogger
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class ComparisonOperatorsTest {

    // ---- < ----

    @Test
    fun `lt basic two args`() {
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(3), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `lt between form three args`() {
        // 1 < 2 < 3 → true
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3)),
            ),
        ).isEqualTo(Value.BoolValue(true))
        // 1 < 1 < 3 → false (first inequality strict)
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(Value.IntValue(1), Value.IntValue(1), Value.IntValue(3)),
            ),
        ).isEqualTo(Value.BoolValue(false))
        // 1 < 5 < 3 → false (second inequality fails)
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(Value.IntValue(1), Value.IntValue(5), Value.IntValue(3)),
            ),
        ).isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `lt coerces strings and bools`() {
        // "1" < 2 → numeric → true
        assertThat(run(ComparisonOperators::opLt, arr(s("1"), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        // false < true → 0 < 1 → true
        assertThat(run(ComparisonOperators::opLt, arr(Value.BoolValue(false), Value.BoolValue(true))))
            .isEqualTo(Value.BoolValue(true))
        // null < 1 → 0 < 1 → true
        assertThat(run(ComparisonOperators::opLt, arr(Value.Null, Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `lt string compared numerically not lexicographically`() {
        // "10" < "9" lexicographically would be true, but we coerce to
        // numbers: 10 < 9 → false. Documented deviation from JS reference.
        assertThat(run(ComparisonOperators::opLt, arr(s("10"), s("9"))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `lt against non-numeric is false via NaN`() {
        // Object can't coerce → NaN; any compare against NaN is false.
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(Value.ObjectValue(emptyMap()), Value.IntValue(1)),
            ),
        ).isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `lt wrong arity is type error`() {
        assertThatThrownBy { run(ComparisonOperators::opLt, arr(Value.IntValue(1))) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
        assertThatThrownBy {
            run(
                ComparisonOperators::opLt,
                arr(
                    Value.IntValue(1),
                    Value.IntValue(2),
                    Value.IntValue(3),
                    Value.IntValue(4),
                ),
            )
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- <= ----

    @Test
    fun `le basic two args`() {
        assertThat(run(ComparisonOperators::opLe, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        // Equal counts as "less than or equal", unlike `<`.
        assertThat(run(ComparisonOperators::opLe, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLe, arr(Value.IntValue(3), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `le between form inclusive`() {
        // 1 <= 2 <= 3 → true
        assertThat(
            run(
                ComparisonOperators::opLe,
                arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3)),
            ),
        ).isEqualTo(Value.BoolValue(true))
        // 1 <= 1 <= 3 → true (boundary inclusive, distinguishes from `<`)
        assertThat(
            run(
                ComparisonOperators::opLe,
                arr(Value.IntValue(1), Value.IntValue(1), Value.IntValue(3)),
            ),
        ).isEqualTo(Value.BoolValue(true))
        // 1 <= 0 <= 3 → false
        assertThat(
            run(
                ComparisonOperators::opLe,
                arr(Value.IntValue(1), Value.IntValue(0), Value.IntValue(3)),
            ),
        ).isEqualTo(Value.BoolValue(false))
    }

    // ---- > ----

    @Test
    fun `gt basic two args`() {
        assertThat(run(ComparisonOperators::opGt, arr(Value.IntValue(2), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opGt, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `gt three args is type error no between form`() {
        // `>` doesn't support a 3-arg between form (matches JS reference).
        assertThatThrownBy {
            run(
                ComparisonOperators::opGt,
                arr(Value.IntValue(3), Value.IntValue(2), Value.IntValue(1)),
            )
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    @Test
    fun `gt one arg is type error`() {
        assertThatThrownBy { run(ComparisonOperators::opGt, arr(Value.IntValue(1))) }
            .isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- >= ----

    @Test
    fun `ge basic two args`() {
        assertThat(run(ComparisonOperators::opGe, arr(Value.IntValue(2), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
        // Equal qualifies, unlike `>`.
        assertThat(run(ComparisonOperators::opGe, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opGe, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `ge three args is type error no between form`() {
        assertThatThrownBy {
            run(
                ComparisonOperators::opGe,
                arr(Value.IntValue(3), Value.IntValue(2), Value.IntValue(1)),
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
