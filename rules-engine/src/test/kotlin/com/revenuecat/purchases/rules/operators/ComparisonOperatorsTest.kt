package com.revenuecat.purchases.rules.operators

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
    fun `lt compares two strings lexicographically`() {
        // Per the JSON Logic spec (ECMAScript Abstract Relational
        // Comparison), two string operands compare lexicographically.
        // "10" < "9" → true because '1' (0x31) < '9' (0x39).
        assertThat(run(ComparisonOperators::opLt, arr(s("10"), s("9"))))
            .isEqualTo(Value.BoolValue(true))
        // Plain alphabetic ordering also flows through.
        assertThat(run(ComparisonOperators::opLt, arr(s("apple"), s("banana"))))
            .isEqualTo(Value.BoolValue(true))
        // Empty string is lex-less than any non-empty string.
        assertThat(run(ComparisonOperators::opLt, arr(s(""), s("a"))))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `lt mixed string and number coerces numerically`() {
        // Mixed types fall through to numeric coercion, NOT lex —
        // `"10" < 9` becomes `10 < 9` → false, while a pure-string
        // compare would have said true. Pins the spec's "only lex when
        // BOTH operands are strings" branch.
        assertThat(run(ComparisonOperators::opLt, arr(s("10"), Value.IntValue(9))))
            .isEqualTo(Value.BoolValue(false))
        // Non-numeric string coerces to NaN → comparison is false.
        assertThat(run(ComparisonOperators::opLt, arr(s("abc"), Value.IntValue(9))))
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

    @Test
    fun `le compares two strings lexicographically inclusive`() {
        // Lex compare under `<=` — equal strings qualify, ordered
        // strings resolve by spec.
        assertThat(run(ComparisonOperators::opLe, arr(s("abc"), s("abc"))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLe, arr(s("9"), s("10"))))
            .isEqualTo(Value.BoolValue(false))
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
    fun `gt compares two strings lexicographically`() {
        // "9" > "10" → true (lex, '9' > '1'). Mirrors the `<` case in
        // reverse and confirms the lex/numeric dispatch covers `>` too.
        assertThat(run(ComparisonOperators::opGt, arr(s("9"), s("10"))))
            .isEqualTo(Value.BoolValue(true))
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
        op: (Value, Value) -> Value,
        args: Value,
    ): Value = op(args, Value.Null)

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
