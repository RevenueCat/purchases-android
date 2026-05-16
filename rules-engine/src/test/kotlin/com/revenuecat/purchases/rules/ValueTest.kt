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
    fun `looseEq arrays structural`() {
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
                Value.ArrayValue(listOf(Value.IntValue(1), Value.FloatValue(2.0))),
            ),
        ).isTrue
        assertThat(
            looseEq(
                Value.ArrayValue(listOf(Value.IntValue(1))),
                Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2))),
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

    // ---- IEEE 754 edge cases ----

    @Test
    fun `NaN never equals NaN under loose or strict eq`() {
        // Inherited from `Double.NaN == Double.NaN` being false per IEEE 754
        // (and matches JS `==` / `===` semantics). Locked down so a future
        // refactor doesn't accidentally special-case NaN-equals-NaN.
        val nan = Value.FloatValue(Double.NaN)
        assertThat(looseEq(nan, nan)).isFalse
        assertThat(strictEq(nan, nan)).isFalse
    }

    @Test
    fun `Infinity equals itself under loose and strict eq`() {
        // `Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY` is true under
        // IEEE 754; pinning so cross-platform drift (Android's
        // `toDoubleOrNull` vs iOS's `Double(String)`) stays visible.
        val inf = Value.FloatValue(Double.POSITIVE_INFINITY)
        assertThat(looseEq(inf, inf)).isTrue
        assertThat(strictEq(inf, inf)).isTrue
    }

    private fun parse(input: String): Value = ValueJsonHelper.fromJsonString(input)
}
