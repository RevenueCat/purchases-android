package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ComparisonOperatorsTest {

    // ---- < ----

    @Test
    fun testLtBasicTwoArgs() {
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(3), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun testLtBetweenFormThreeArgs() {
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
    fun testLtCoercesStringsAndBools() {
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
    fun testLtComparesTwoStringsLexicographically() {
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
    fun testLtMixedStringAndNumberCoercesNumerically() {
        // Mixed types fall through to numeric coercion, NOT lex — `"10" < 9`
        // becomes `10 < 9` → false, while a pure-string compare would have
        // said true. This is the JS spec's "only lex when BOTH are strings"
        // branch.
        assertThat(run(ComparisonOperators::opLt, arr(s("10"), Value.IntValue(9))))
            .isEqualTo(Value.BoolValue(false))
        // Non-numeric string coerces to NaN → comparison is false.
        assertThat(run(ComparisonOperators::opLt, arr(s("abc"), Value.IntValue(9))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun testLtAgainstNonNumericIsFalseViaNan() {
        // Object can't coerce → NaN; any compare against NaN is false.
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(Value.ObjectValue(emptyMap()), Value.IntValue(1)),
            ),
        ).isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun testNullOperandsCoerceToZero() {
        // Number(null) is 0; object/array operands still hit NaN.
        assertThat(run(ComparisonOperators::opLt, arr(Value.Null, Value.Null)))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opLe, arr(Value.Null, Value.Null)))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opGt, arr(Value.Null, Value.Null)))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opGe, arr(Value.Null, Value.Null)))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLt, arr(Value.Null, Value.ObjectValue(emptyMap()))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opLt, arr(Value.ObjectValue(emptyMap()), Value.Null)))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opLt, arr(Value.Null, Value.ArrayValue(emptyList()))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(
            run(
                ComparisonOperators::opLe,
                arr(Value.IntValue(0), Value.Null, Value.IntValue(1)),
            ),
        ).isEqualTo(Value.BoolValue(true))
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(Value.IntValue(0), Value.Null, Value.IntValue(1)),
            ),
        ).isEqualTo(Value.BoolValue(false))
    }

    // `json-logic-js` declares `<` as `function(a, b, c)` so missing
    // operands resolve to `undefined`, which coerces to `NaN`; any
    // comparison against `NaN` is `false`.
    @Test
    fun testLtMissingOperandsCompareAgainstNaN() {
        assertThat(run(ComparisonOperators::opLt, arr(Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opLt, arr()))
            .isEqualTo(Value.BoolValue(false))
    }

    // `json-logic-js`'s `<` ignores arguments past the third (JS
    // silently drops named parameters' overflow).
    @Test
    fun testLtIgnoresArgsBeyondThird() {
        assertThat(
            run(
                ComparisonOperators::opLt,
                arr(
                    Value.IntValue(1),
                    Value.IntValue(2),
                    Value.IntValue(3),
                    Value.IntValue(0),
                ),
            ),
        ).isEqualTo(Value.BoolValue(true))
    }

    // ---- <= ----

    @Test
    fun testLeBasicTwoArgs() {
        assertThat(run(ComparisonOperators::opLe, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        // Equal counts as "less than or equal", unlike `<`.
        assertThat(run(ComparisonOperators::opLe, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLe, arr(Value.IntValue(3), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun testLeComparesTwoStringsLexicographicallyInclusive() {
        // Lex compare under `<=` — equal strings qualify, ordered strings
        // resolve by spec.
        assertThat(run(ComparisonOperators::opLe, arr(s("abc"), s("abc"))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opLe, arr(s("9"), s("10"))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun testLeBetweenFormInclusive() {
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
    fun testGtBasicTwoArgs() {
        assertThat(run(ComparisonOperators::opGt, arr(Value.IntValue(2), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opGt, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun testGtComparesTwoStringsLexicographically() {
        // "9" > "10" → true (lex, '9' > '1'). Mirrors the `<` case in
        // reverse and confirms the lex/numeric dispatch covers `>` too.
        assertThat(run(ComparisonOperators::opGt, arr(s("9"), s("10"))))
            .isEqualTo(Value.BoolValue(true))
    }

    // `>` is `function(a, b)` in `json-logic-js`, so extras are
    /// silently discarded — there is no 3-arg between form.
    @Test
    fun testGtIgnoresArgsBeyondSecond() {
        assertThat(
            run(
                ComparisonOperators::opGt,
                arr(Value.IntValue(3), Value.IntValue(2), Value.IntValue(1)),
            ),
        ).isEqualTo(Value.BoolValue(true))
    }

    // Missing second operand resolves to `undefined`, coerces to
    /// `NaN`, and any comparison against `NaN` is `false`.
    @Test
    fun testGtMissingOperandsCompareAgainstNaN() {
        assertThat(run(ComparisonOperators::opGt, arr(Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(false))
        assertThat(run(ComparisonOperators::opGt, arr()))
            .isEqualTo(Value.BoolValue(false))
    }

    // ---- >= ----

    @Test
    fun testGeBasicTwoArgs() {
        assertThat(run(ComparisonOperators::opGe, arr(Value.IntValue(2), Value.IntValue(1))))
            .isEqualTo(Value.BoolValue(true))
        // Equal qualifies, unlike `>`.
        assertThat(run(ComparisonOperators::opGe, arr(Value.IntValue(2), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(true))
        assertThat(run(ComparisonOperators::opGe, arr(Value.IntValue(1), Value.IntValue(2))))
            .isEqualTo(Value.BoolValue(false))
    }

    // `>=` is `function(a, b)` in `json-logic-js`, so extras are
    /// silently discarded — there is no 3-arg between form.
    @Test
    fun testGeIgnoresArgsBeyondSecond() {
        assertThat(
            run(
                ComparisonOperators::opGe,
                arr(Value.IntValue(3), Value.IntValue(2), Value.IntValue(1)),
            ),
        ).isEqualTo(Value.BoolValue(true))
    }

    // ---- helpers ----

    private fun run(
        op: (Value, Value) -> Value,
        args: Value,
    ): Value = op(args, Value.Null)

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
