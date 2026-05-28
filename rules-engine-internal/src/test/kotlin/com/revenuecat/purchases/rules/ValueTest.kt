package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.ValueJsonHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ValueTest {

    // ---- JSON parsing (test helper) ----

    @Test
    fun `parses primitives`() {
        assertThat(parse("null")).isEqualTo(Value.Null)
        assertThat(parse("true")).isEqualTo(Value.BoolValue(true))
        assertThat(parse("false")).isEqualTo(Value.BoolValue(false))
        assertThat(parse("42")).isEqualTo(Value.IntValue(42))
        assertThat(parse("-7")).isEqualTo(Value.IntValue(-7))
        assertThat(parse("2.5")).isEqualTo(Value.FloatValue(2.5))
        assertThat(parse("\"hello\"")).isEqualTo(Value.StringValue("hello"))
    }

    @Test
    fun `parses array and object`() {
        val array = parse("[1, \"two\", true, null]")
        assertThat(array).isEqualTo(
            Value.ArrayValue(
                listOf(
                    Value.IntValue(1),
                    Value.StringValue("two"),
                    Value.BoolValue(true),
                    Value.Null,
                ),
            ),
        )

        val obj = parse("""{"a": 1, "b": "two"}""")
        require(obj is Value.ObjectValue)
        assertThat(obj.entries["a"]).isEqualTo(Value.IntValue(1))
        assertThat(obj.entries["b"]).isEqualTo(Value.StringValue("two"))
    }

    @Test(expected = RuleError.Parse::class)
    fun `parse error for malformed JSON`() {
        parse("{not json")
    }

    @Test
    fun `integer-looking numbers are int not float`() {
        assertThat(parse("0")).isEqualTo(Value.IntValue(0))
        assertThat(parse("100")).isEqualTo(Value.IntValue(100))
        assertThat(parse("100.0")).isEqualTo(Value.FloatValue(100.0))
    }

    // ---- truthiness ----

    @Test
    @Suppress("LongMethod")
    fun `truthiness table`() {
        assertThat(Value.Null.isTruthy).isFalse
        assertThat(Value.BoolValue(false).isTruthy).isFalse
        assertThat(Value.BoolValue(true).isTruthy).isTrue
        assertThat(Value.IntValue(0).isTruthy).isFalse
        assertThat(Value.IntValue(1).isTruthy).isTrue
        assertThat(Value.IntValue(-1).isTruthy).isTrue
        assertThat(Value.FloatValue(0.0).isTruthy).isFalse
        assertThat(Value.FloatValue(Double.NaN).isTruthy).isFalse
        assertThat(Value.FloatValue(0.5).isTruthy).isTrue
        assertThat(Value.StringValue("").isTruthy).isFalse
        assertThat(Value.StringValue("0").isTruthy).isTrue // non-empty string is truthy
        assertThat(Value.ArrayValue(emptyList()).isTruthy).isFalse
        assertThat(Value.ArrayValue(listOf(Value.BoolValue(false))).isTruthy).isTrue // non-empty array
        assertThat(Value.ObjectValue(emptyMap()).isTruthy).isTrue // objects always truthy
    }

    // ---- loose equality ----

    @Test
    fun `looseEq same type`() {
        assertThat(looseEq(Value.IntValue(1), Value.IntValue(1))).isTrue
        assertThat(looseEq(Value.IntValue(1), Value.IntValue(2))).isFalse
        assertThat(looseEq(Value.StringValue("abc"), Value.StringValue("abc"))).isTrue
        assertThat(looseEq(Value.BoolValue(true), Value.BoolValue(true))).isTrue
    }

    @Test
    fun `looseEq int vs float`() {
        assertThat(looseEq(Value.IntValue(1), Value.FloatValue(1.0))).isTrue
        assertThat(looseEq(Value.IntValue(1), Value.FloatValue(1.5))).isFalse
    }

    @Test
    fun `looseEq bool vs number`() {
        assertThat(looseEq(Value.BoolValue(true), Value.IntValue(1))).isTrue
        assertThat(looseEq(Value.BoolValue(false), Value.IntValue(0))).isTrue
        assertThat(looseEq(Value.BoolValue(true), Value.FloatValue(1.0))).isTrue
        assertThat(looseEq(Value.BoolValue(true), Value.IntValue(2))).isFalse
    }

    @Test
    fun `looseEq string vs number`() {
        assertThat(looseEq(Value.StringValue("1"), Value.IntValue(1))).isTrue
        assertThat(looseEq(Value.StringValue("1.5"), Value.FloatValue(1.5))).isTrue
        assertThat(looseEq(Value.StringValue("hello"), Value.IntValue(0))).isFalse
    }

    @Test
    fun `looseEq null only equals null`() {
        assertThat(looseEq(Value.Null, Value.Null)).isTrue
        assertThat(looseEq(Value.Null, Value.IntValue(0))).isFalse
        assertThat(looseEq(Value.Null, Value.BoolValue(false))).isFalse
        assertThat(looseEq(Value.Null, Value.StringValue(""))).isFalse
    }

    @Test
    fun `looseEq array vs array is always false`() {
        // JS abstract equality uses reference identity for arrays —
        // `[1] == [1]` is `false`. Without reference identity, two
        // distinct array operands always compare unequal.
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
                Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
            ),
        ).isFalse
        assertThat(
            looseEq(Value.ArrayValue(emptyList()), Value.ArrayValue(emptyList())),
        ).isFalse
    }

    @Test
    fun `looseEq object vs object is always false`() {
        // Same reference-equality rule as arrays — `{a:1} == {a:1}` is
        // `false` in JS, regardless of structure.
        assertThat(
            looseEq(
                Value.ObjectValue(mapOf("a" to Value.IntValue(1), "b" to Value.StringValue("x"))),
                Value.ObjectValue(mapOf("a" to Value.IntValue(1), "b" to Value.StringValue("x"))),
            ),
        ).isFalse
        assertThat(
            looseEq(Value.ObjectValue(emptyMap()), Value.ObjectValue(emptyMap())),
        ).isFalse
    }

    // ---- loose equality: JS array/object stringify coercion ----

    @Test
    fun `looseEq array coerces to JS string against string`() {
        // JS abstract equality: `Array.prototype.toString()` is invoked,
        // then the comparison falls through to string-vs-string.
        // Reference: `[1] == "1"` → true, `[1, 2] == "1,2"` → true.
        assertThat(looseEq(Value.ArrayValue(listOf(Value.IntValue(1))), Value.StringValue("1"))).isTrue
        assertThat(looseEq(Value.StringValue("1"), Value.ArrayValue(listOf(Value.IntValue(1))))).isTrue
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
                Value.StringValue("1,2"),
            ),
        ).isTrue
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.StringValue("a"), Value.StringValue("b"))),
                Value.StringValue("a,b"),
            ),
        ).isTrue
        assertThat(looseEq(Value.ArrayValue(emptyList()), Value.StringValue(""))).isTrue
        // Non-matching content still compares unequal.
        assertThat(looseEq(Value.ArrayValue(listOf(Value.IntValue(1))), Value.StringValue("2"))).isFalse
    }

    @Test
    fun `looseEq array elements render JS null as empty string`() {
        // `[null].toString()` is `""` (not `"null"`), and
        // `[null, 1].toString()` is `",1"`. The element-stringify rule
        // is JS-specific; pin it directly.
        assertThat(looseEq(Value.ArrayValue(listOf(Value.Null)), Value.StringValue(""))).isTrue
        assertThat(
            looseEq(Value.ArrayValue(listOf(Value.Null, Value.IntValue(1))), Value.StringValue(",1")),
        ).isTrue
        assertThat(
            looseEq(Value.ArrayValue(listOf(Value.Null, Value.Null)), Value.StringValue(",")),
        ).isTrue
    }

    @Test
    fun `looseEq array recurses into nested arrays`() {
        // `[[1, 2], 3].toString()` flattens to `"1,2,3"` — children
        // recurse through the same join.
        assertThat(
            looseEq(
                Value.ArrayValue(
                    listOf(
                        Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
                        Value.IntValue(3),
                    ),
                ),
                Value.StringValue("1,2,3"),
            ),
        ).isTrue
    }

    @Test
    fun `looseEq array coerces through numeric fallback`() {
        // After ToPrimitive, the recursion may hit the
        // string-vs-number numeric fallback. Reference:
        // `[1] == 1` → true, `[] == 0` → true, `[0] == false` → true.
        assertThat(looseEq(Value.ArrayValue(listOf(Value.IntValue(1))), Value.IntValue(1))).isTrue
        assertThat(looseEq(Value.ArrayValue(emptyList()), Value.IntValue(0))).isTrue
        assertThat(looseEq(Value.ArrayValue(listOf(Value.IntValue(0))), Value.BoolValue(false))).isTrue
        assertThat(
            looseEq(Value.ArrayValue(listOf(Value.FloatValue(1.5))), Value.FloatValue(1.5)),
        ).isTrue
        // No spurious matches when the stringified array isn't numeric.
        assertThat(
            looseEq(Value.ArrayValue(listOf(Value.StringValue("hello"))), Value.IntValue(0)),
        ).isFalse
    }

    @Test
    fun `looseEq array renders JS-specific floats correctly`() {
        // `String(1.0)` is `"1"` (no decimal), `String(NaN)` is `"NaN"`,
        // `String(Infinity)` is `"Infinity"`. These show up only via the
        // array stringify path — `==` against a bare `Double.NaN` would
        // still be `false` because NaN isn't equal to itself.
        assertThat(
            looseEq(Value.ArrayValue(listOf(Value.FloatValue(1.0))), Value.StringValue("1")),
        ).isTrue
        assertThat(
            looseEq(Value.ArrayValue(listOf(Value.FloatValue(Double.NaN))), Value.StringValue("NaN")),
        ).isTrue
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.FloatValue(Double.POSITIVE_INFINITY))),
                Value.StringValue("Infinity"),
            ),
        ).isTrue
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.FloatValue(Double.NEGATIVE_INFINITY))),
                Value.StringValue("-Infinity"),
            ),
        ).isTrue
    }

    @Test
    fun `looseEq object coerces to object Object string`() {
        // JS `Object.prototype.toString.call({a: 1})` is
        // `"[object Object]"`, so any object compared against that
        // exact string is loosely equal.
        assertThat(
            looseEq(
                Value.ObjectValue(mapOf("a" to Value.IntValue(1), "b" to Value.IntValue(2))),
                Value.StringValue("[object Object]"),
            ),
        ).isTrue
        assertThat(
            looseEq(Value.StringValue("[object Object]"), Value.ObjectValue(emptyMap())),
        ).isTrue
        assertThat(
            looseEq(
                Value.ObjectValue(mapOf("a" to Value.IntValue(1), "b" to Value.IntValue(2))),
                Value.StringValue("{a:1,b:2}"),
            ),
        ).isFalse
    }

    @Test
    fun `looseEq array vs object is always false`() {
        // Two compound operands of different shape: JS uses reference
        // identity (false). Both ToPrimitive results are strings that
        // can't ever match (`"1,2"` vs `"[object Object]"`).
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
                Value.ObjectValue(mapOf("a" to Value.IntValue(1), "b" to Value.IntValue(2))),
            ),
        ).isFalse
    }

    // ---- strict equality ----

    @Test
    fun `strictEq requires same value or compatible numeric`() {
        assertThat(strictEq(Value.IntValue(1), Value.IntValue(1))).isTrue
        assertThat(strictEq(Value.IntValue(1), Value.FloatValue(1.0))).isTrue // int/float bridge
        assertThat(strictEq(Value.IntValue(1), Value.StringValue("1"))).isFalse
        assertThat(strictEq(Value.BoolValue(true), Value.IntValue(1))).isFalse
        assertThat(strictEq(Value.Null, Value.BoolValue(false))).isFalse
    }

    @Test
    fun `strictEq arrays and objects always false`() {
        // JS `===` for arrays/objects is reference identity — same
        // rationale as `looseEq`. Without references, distinct operands
        // always compare unequal.
        assertThat(
            strictEq(
                Value.ArrayValue(listOf(Value.IntValue(1))),
                Value.ArrayValue(listOf(Value.IntValue(1))),
            ),
        ).isFalse
        assertThat(
            strictEq(Value.ArrayValue(emptyList()), Value.ArrayValue(emptyList())),
        ).isFalse
        assertThat(
            strictEq(
                Value.ObjectValue(mapOf("a" to Value.IntValue(1))),
                Value.ObjectValue(mapOf("a" to Value.IntValue(1))),
            ),
        ).isFalse
        assertThat(
            strictEq(Value.ObjectValue(emptyMap()), Value.ObjectValue(emptyMap())),
        ).isFalse
    }

    // ---- NaN / Infinity edge cases ----

    @Test
    fun `NaN is falsy and never equals itself`() {
        // IEEE 754: any comparison involving NaN is false, including NaN==NaN.
        val nan = Value.FloatValue(Double.NaN)
        assertThat(nan.isTruthy).isFalse
        assertThat(looseEq(nan, nan)).isFalse
        assertThat(strictEq(nan, nan)).isFalse
        assertThat(looseEq(nan, Value.IntValue(0))).isFalse
        assertThat(looseEq(nan, Value.FloatValue(0.0))).isFalse
    }

    @Test
    fun `Infinity is truthy and compares by IEEE754`() {
        assertThat(Value.FloatValue(Double.POSITIVE_INFINITY).isTruthy).isTrue
        assertThat(Value.FloatValue(Double.NEGATIVE_INFINITY).isTruthy).isTrue

        val inf = Value.FloatValue(Double.POSITIVE_INFINITY)
        assertThat(looseEq(inf, inf)).isTrue
        assertThat(strictEq(inf, inf)).isTrue
        assertThat(looseEq(inf, Value.FloatValue(Double.NEGATIVE_INFINITY))).isFalse

        // Cross-type: +Infinity never numerically equals a finite int.
        assertThat(looseEq(inf, Value.IntValue(Long.MAX_VALUE))).isFalse
    }

    // ---- jsParseFloat (direct helper coverage) ----

    /**
     * Pins [jsParseFloat] / `parseFloatPrefix` independently of arithmetic
     * operators so two compensating operator bugs can't hide a coercion
     * regression in the shared helper.
     */
    @Test
    fun `jsParseFloat matches spec`() {
        // Numbers pass through without stringification.
        assertThat(jsParseFloat(Value.IntValue(42))).isEqualTo(42.0)
        assertThat(jsParseFloat(Value.FloatValue(2.5))).isEqualTo(2.5)

        // Valid numeric strings, including scientific notation.
        assertThat(jsParseFloat(Value.StringValue("2.5"))).isEqualTo(2.5)
        assertThat(jsParseFloat(Value.StringValue("1e3"))).isEqualTo(1000.0)
        assertThat(jsParseFloat(Value.StringValue("1.5e2"))).isEqualTo(150.0)
        assertThat(jsParseFloat(Value.StringValue("-2.5e-1"))).isEqualTo(-0.25)

        // Leading whitespace and longest-prefix parsing.
        assertThat(jsParseFloat(Value.StringValue("  7"))).isEqualTo(7.0)
        assertThat(jsParseFloat(Value.StringValue("3.14abc"))).isEqualTo(3.14)

        // Infinity literal (distinct from overflow).
        assertThat(jsParseFloat(Value.StringValue("Infinity"))).isEqualTo(Double.POSITIVE_INFINITY)
        assertThat(jsParseFloat(Value.StringValue("-Infinity"))).isEqualTo(Double.NEGATIVE_INFINITY)

        // Stringify-then-parse path for compounds.
        assertThat(jsParseFloat(Value.ArrayValue(listOf(Value.IntValue(1))))).isEqualTo(1.0)
        assertThat(
            jsParseFloat(Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2)))),
        ).isEqualTo(1.0)

        // Non-numeric after stringify → NaN.
        assertThat(jsParseFloat(Value.Null).isNaN()).isTrue
        assertThat(jsParseFloat(Value.BoolValue(true)).isNaN()).isTrue
        assertThat(jsParseFloat(Value.StringValue("")).isNaN()).isTrue
        assertThat(jsParseFloat(Value.StringValue("true")).isNaN()).isTrue
        assertThat(jsParseFloat(Value.ObjectValue(emptyMap())).isNaN()).isTrue
        assertThat(jsParseFloat(Value.StringValue("abc")).isNaN()).isTrue
    }

    @Test
    fun `jsNumberString falls through to Kotlin Double toString for out-of-Long range`() {
        // Last whole number that still round-trips through Long — fast path,
        // matches JS (`String(1e18) === "1000000000000000000"`).
        assertThat(jsString(Value.FloatValue(1e18))).isEqualTo("1000000000000000000")

        // Spec-divergence pin: see KDoc on jsNumberString. JS renders `1e19`
        // as `"10000000000000000000"`; Kotlin uses `"1.0E19"`.
        assertThat(jsString(Value.FloatValue(1e19))).isEqualTo("1.0E19")
    }

    private fun parse(input: String): Value = ValueJsonHelper.fromJsonString(input)
}
