package com.revenuecat.purchases.rules.operators

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
        // {"+": [true]} → NaN. JS uses parseFloat(value), and
        // parseFloat("true") is NaN — bool coercion through arithmetic
        // is *not* the same as Number(true) === 1.
        val boolResult = run(ArithmeticOperators::opAdd, arr(Value.BoolValue(true))) as Value.FloatValue
        assertThat(boolResult.value.isNaN()).isTrue
    }

    @Test
    fun `add coerces numeric strings`() {
        // "1" + 1 → 2 — parseFloat("1") is 1.
        assertThat(run(ArithmeticOperators::opAdd, arr(s("1"), Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(2.0))
        // "3.14abc" + 0 → 3.14 — parseFloat parses the longest numeric
        // prefix, so trailing junk doesn't poison the result. This is one
        // of the visible side-effects of `+` using parseFloat instead of
        // Number().
        assertThat(run(ArithmeticOperators::opAdd, arr(s("3.14abc"), Value.IntValue(0))))
            .isEqualTo(Value.FloatValue(3.14))
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

    /**
     * `{"+": []}` returns `0` per `json-logic-js`
     * (`Array.prototype.reduce(fn, 0)` with no operands returns the seed).
     */
    @Test
    fun `add zero args is zero`() {
        assertThat(run(ArithmeticOperators::opAdd, arr()))
            .isEqualTo(Value.FloatValue(0.0))
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

    /**
     * `{"*": [a]}` returns `a` unchanged per `json-logic-js` (single-arg
     * `Array.prototype.reduce` without seed returns the lone element
     * without invoking the reducer, so parseFloat is never applied).
     */
    @Test
    fun `mul one arg returns value unchanged`() {
        assertThat(run(ArithmeticOperators::opMul, arr(Value.IntValue(5))))
            .isEqualTo(Value.IntValue(5))
        assertThat(run(ArithmeticOperators::opMul, arr(s("3.14abc"))))
            .isEqualTo(s("3.14abc"))
        assertThat(run(ArithmeticOperators::opMul, arr(Value.Null)))
            .isEqualTo(Value.Null)
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

    /**
     * `{"-": [a, b, c, ...]}` ignores everything past the first two
     * operands per `json-logic-js` (`function(a, b)` only references the
     * first two `arguments`).
     */
    @Test
    fun `sub extra args ignored`() {
        assertThat(
            run(
                ArithmeticOperators::opSub,
                arr(Value.IntValue(10), Value.IntValue(3), Value.IntValue(99)),
            ),
        ).isEqualTo(Value.FloatValue(7.0))
    }

    /**
     * `{"-": []}` returns `NaN` per `json-logic-js` (`a` is undefined,
     * `b === undefined` falls into the unary path → `-undefined` → NaN).
     */
    @Test
    fun `sub zero args is NaN`() {
        assertNaN(run(ArithmeticOperators::opSub, arr()))
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

    /**
     * `n / 0` follows IEEE 754: positive dividend → `+Infinity`, negative
     * dividend → `-Infinity`, `0 / 0` → `NaN`. Matches `json-logic-js`,
     * which delegates to native JS `/` (no short-circuit).
     */
    @Test
    fun `div by zero follows IEEE 754`() {
        // 1 / 0 → +Infinity (covers both IntValue(0) and FloatValue(0.0)
        // divisors).
        assertThat(unwrapFloat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(1), Value.IntValue(0)))))
            .isEqualTo(Double.POSITIVE_INFINITY)
        assertThat(unwrapFloat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(1), Value.FloatValue(0.0)))))
            .isEqualTo(Double.POSITIVE_INFINITY)
        // -1 / 0 → -Infinity.
        assertThat(unwrapFloat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(-1), Value.IntValue(0)))))
            .isEqualTo(Double.NEGATIVE_INFINITY)
        // 0 / 0 → NaN.
        assertNaN(run(ArithmeticOperators::opDiv, arr(Value.IntValue(0), Value.IntValue(0))))
    }

    /**
     * `{"/": [a]}` and `{"/": [a, b, c, ...]}` mirror `json-logic-js`,
     * which uses `function(a, b) { return a / b; }`. Missing operands are
     * `undefined`, so `a / undefined` → `NaN`; extra operands are
     * ignored.
     */
    @Test
    fun `div only uses first two operands`() {
        assertNaN(run(ArithmeticOperators::opDiv, arr(Value.IntValue(1))))
        assertNaN(run(ArithmeticOperators::opDiv, arr()))
        assertThat(
            run(
                ArithmeticOperators::opDiv,
                arr(Value.IntValue(10), Value.IntValue(2), Value.IntValue(99)),
            ),
        ).isEqualTo(Value.FloatValue(5.0))
    }

    // ---- % ----

    @Test
    fun `mod basic`() {
        assertThat(run(ArithmeticOperators::opMod, arr(Value.IntValue(10), Value.IntValue(3))))
            .isEqualTo(Value.FloatValue(1.0))
    }

    /**
     * `n % 0` is always `NaN` per IEEE 754. Matches `json-logic-js`,
     * which delegates to native JS `%` (no short-circuit).
     */
    @Test
    fun `mod by zero is NaN`() {
        assertNaN(run(ArithmeticOperators::opMod, arr(Value.IntValue(7), Value.IntValue(0))))
        assertNaN(run(ArithmeticOperators::opMod, arr(Value.IntValue(0), Value.IntValue(0))))
    }

    /**
     * Mirror of `div only uses first two operands` for `%`.
     */
    @Test
    fun `mod only uses first two operands`() {
        assertNaN(run(ArithmeticOperators::opMod, arr(Value.IntValue(1))))
        assertNaN(run(ArithmeticOperators::opMod, arr()))
        assertThat(
            run(
                ArithmeticOperators::opMod,
                arr(Value.IntValue(7), Value.IntValue(3), Value.IntValue(99)),
            ),
        ).isEqualTo(Value.FloatValue(1.0))
    }

    // ---- coercion semantics (`+`/`*` use parseFloat, others use Number) ----

    /**
     * `+` and `*` coerce every operand through JS `parseFloat(value)`.
     * `parseFloat` first calls `String(value)`, then parses the longest
     * numeric prefix — so `null` becomes the string `"null"` (→ NaN),
     * bools become `"true"` / `"false"` (→ NaN), the empty string
     * becomes the empty string (→ NaN), and `[1,2]` becomes `"1,2"`
     * (parses as `1`). This is asymmetric with `-` / `/` / `%`, which
     * use `Number(value)` (see `sub div and mod use Number per spec`).
     */
    @Test
    fun `add and mul use parseFloat per spec`() {
        // null + 1 → NaN (parseFloat("null") is NaN).
        assertNaN(run(ArithmeticOperators::opAdd, arr(Value.Null, Value.IntValue(1))))
        // null * 1 → NaN.
        assertNaN(run(ArithmeticOperators::opMul, arr(Value.Null, Value.IntValue(1))))

        // true + 1 → NaN, false + 1 → NaN. Bools never bridge through
        // `+` / `*` even though Number(true) === 1.
        assertNaN(run(ArithmeticOperators::opAdd, arr(Value.BoolValue(true), Value.IntValue(1))))
        assertNaN(run(ArithmeticOperators::opAdd, arr(Value.BoolValue(false), Value.IntValue(1))))

        // "" + 1 → NaN (parseFloat("") is NaN, unlike Number("") === 0).
        assertNaN(run(ArithmeticOperators::opAdd, arr(s(""), Value.IntValue(1))))

        // [1] + 1 → 2 — array stringifies to "1", parseFloat → 1.
        assertThat(run(ArithmeticOperators::opAdd, arr(arr(Value.IntValue(1)), Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(2.0))
        // [1, 2] + 0 → 1 — array stringifies to "1,2", parseFloat parses
        // the leading "1" prefix and stops at the comma.
        assertThat(
            run(
                ArithmeticOperators::opAdd,
                arr(arr(Value.IntValue(1), Value.IntValue(2)), Value.IntValue(0)),
            ),
        ).isEqualTo(Value.FloatValue(1.0))
        // {} + 1 → NaN — objects stringify to "[object Object]".
        assertNaN(run(ArithmeticOperators::opAdd, arr(Value.ObjectValue(emptyMap()), Value.IntValue(1))))
    }

    /**
     * `-`, `/`, `%` delegate to native JS arithmetic, which calls
     * `Number(value)` on each operand. `Number()` is stricter about
     * numeric strings (`"3.14abc"` → NaN) but more permissive about
     * `null` / bools / empty strings (all → 0 or 1) than `parseFloat`.
     * Arrays / objects coerce via `ToPrimitive("number")` → `toString`
     * → recurse, so `[]` → 0, `[1]` → 1, `[1,2]` → NaN.
     */
    @Test
    fun `sub div and mod use Number per spec`() {
        // null is 0 across all three ops.
        assertThat(run(ArithmeticOperators::opSub, arr(Value.Null, Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(-1.0))
        // unary; -0.0 == 0.0 by IEEE 754
        assertThat(unwrapFloat(run(ArithmeticOperators::opSub, arr(Value.Null))))
            .isEqualTo(0.0)
        assertThat(run(ArithmeticOperators::opDiv, arr(Value.Null, Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(0.0))
        assertThat(run(ArithmeticOperators::opMod, arr(Value.Null, Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(0.0))

        // 1 / null → divisor coerces to 0 → +Infinity (IEEE 754, see
        // `div by zero follows IEEE 754` for the broader pinning).
        assertThat(unwrapFloat(run(ArithmeticOperators::opDiv, arr(Value.IntValue(1), Value.Null))))
            .isEqualTo(Double.POSITIVE_INFINITY)
        // 1 % null → divisor coerces to 0 → NaN.
        assertNaN(run(ArithmeticOperators::opMod, arr(Value.IntValue(1), Value.Null)))

        // Bools coerce to 0 / 1 (Number(true) === 1, Number(false) === 0).
        assertThat(run(ArithmeticOperators::opSub, arr(Value.BoolValue(true), Value.BoolValue(false))))
            .isEqualTo(Value.FloatValue(1.0))

        // Empty string coerces to 0.
        assertThat(run(ArithmeticOperators::opSub, arr(s(""), Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(-1.0))

        // [] - 1 → -1 (toString → "" → 0).
        assertThat(run(ArithmeticOperators::opSub, arr(arr(), Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(-1.0))
        // [1] - 1 → 0 (toString → "1" → 1).
        assertThat(run(ArithmeticOperators::opSub, arr(arr(Value.IntValue(1)), Value.IntValue(1))))
            .isEqualTo(Value.FloatValue(0.0))
        // [1, 2] - 0 → NaN (toString → "1,2" → NaN: whole-string parse
        // fails because of the comma).
        assertNaN(
            run(
                ArithmeticOperators::opSub,
                arr(arr(Value.IntValue(1), Value.IntValue(2)), Value.IntValue(0)),
            ),
        )
        // {} - 0 → NaN (toString → "[object Object]" → NaN).
        assertNaN(run(ArithmeticOperators::opSub, arr(Value.ObjectValue(emptyMap()), Value.IntValue(0))))
    }

    // ---- helpers ----

    private fun run(
        op: (Value, Value) -> Value,
        args: Value,
    ): Value = op(args, Value.Null)

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())

    private fun s(literal: String): Value = Value.StringValue(literal)

    private fun unwrapFloat(value: Value): Double {
        assertThat(value).isInstanceOf(Value.FloatValue::class.java)
        return (value as Value.FloatValue).value
    }

    private fun assertNaN(value: Value) {
        assertThat(value).isInstanceOf(Value.FloatValue::class.java)
        assertThat((value as Value.FloatValue).value.isNaN())
            .withFailMessage("expected NaN, got ${value.value}")
            .isTrue
    }
}
