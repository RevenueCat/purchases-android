package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.PrintlnLogger
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

@Suppress("LargeClass")
class StringArrayOperatorsTest {

    // ---- in ----

    @Test
    fun `in substring match for string haystack`() {
        val out = StringArrayOperators.opIn(
            arr(s("bar"), s("foobar")),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `in substring no match for string haystack`() {
        val out = StringArrayOperators.opIn(
            arr(s("baz"), s("foobar")),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `in array membership strict types`() {
        val out = StringArrayOperators.opIn(
            arr(s("US"), arr(s("US"), s("CA"), s("MX"))),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `in array membership uses looseEq`() {
        // 5 vs "5" — looseEq says equal. Documented deviation from JS
        // reference's strict `===` membership.
        val out = StringArrayOperators.opIn(
            arr(Value.IntValue(5), arr(s("5"), s("6"))),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `in non-string needle in string haystack is false`() {
        // We only support substring search when both sides are strings.
        val out = StringArrayOperators.opIn(
            arr(Value.IntValue(5), s("12345")),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `in unsupported haystack type is false`() {
        // Null, numbers, bools, objects all return false (no `indexOf`
        // in JS).
        val unsupported = listOf(
            Value.Null,
            Value.IntValue(123),
            Value.BoolValue(true),
            Value.ObjectValue(emptyMap()),
        )
        for (haystack in unsupported) {
            val out = StringArrayOperators.opIn(
                arr(s("x"), haystack),
                Value.Null,
                PrintlnLogger,
            )
            assertThat(out).isEqualTo(Value.BoolValue(false))
        }
    }

    @Test
    fun `in arity mismatch is type error`() {
        assertThatThrownBy {
            StringArrayOperators.opIn(arr(s("only-one")), Value.Null, PrintlnLogger)
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- cat ----

    @Test
    fun `cat concatenates strings`() {
        val out = StringArrayOperators.opCat(
            arr(s("I love "), s("pie")),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("I love pie"))
    }

    @Test
    fun `cat stringifies mixed operand types`() {
        val out = StringArrayOperators.opCat(
            arr(
                s("count="),
                Value.IntValue(7),
                s(", active="),
                Value.BoolValue(true),
            ),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("count=7, active=true"))
    }

    @Test
    fun `cat zero args returns empty string`() {
        val out = StringArrayOperators.opCat(arr(), Value.Null, PrintlnLogger)
        assertThat(out).isEqualTo(s(""))
    }

    @Test
    fun `cat singleton shorthand is supported`() {
        // `{"cat": "hello"}` ≡ `{"cat": ["hello"]}` per `argsAsList`.
        val out = StringArrayOperators.opCat(s("hello"), Value.Null, PrintlnLogger)
        assertThat(out).isEqualTo(s("hello"))
    }

    @Test
    fun `cat stringifies null as string null`() {
        val out = StringArrayOperators.opCat(
            arr(s("x="), Value.Null),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("x=null"))
    }

    @Test
    fun `cat stringifies array with comma join`() {
        // Mirrors JS `Array.prototype.toString` — `[1,2,3].toString()`
        // is "1,2,3".
        val out = StringArrayOperators.opCat(
            arr(s("vals="), arr(Value.IntValue(1), Value.IntValue(2))),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("vals=1,2"))
    }

    // ---- substr ----

    @Test
    fun `substr two args extracts to end`() {
        val out = StringArrayOperators.opSubstr(
            arr(s("hello"), Value.IntValue(1)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("ello"))
    }

    @Test
    fun `substr three args extracts fixed length`() {
        val out = StringArrayOperators.opSubstr(
            arr(s("hello"), Value.IntValue(1), Value.IntValue(3)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("ell"))
    }

    @Test
    fun `substr negative start counts from end`() {
        val out = StringArrayOperators.opSubstr(
            arr(s("hello"), Value.IntValue(-2)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("lo"))
    }

    @Test
    fun `substr negative length drops from right`() {
        // {"substr": ["hello", 1, -2]}:
        //   step 1: "hello".substr(1) = "ello"
        //   step 2: "ello".substr(0, len("ello") + (-2)) = "el"
        val out = StringArrayOperators.opSubstr(
            arr(s("hello"), Value.IntValue(1), Value.IntValue(-2)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("el"))
    }

    @Test
    fun `substr start past end returns empty`() {
        val out = StringArrayOperators.opSubstr(
            arr(s("abc"), Value.IntValue(10)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s(""))
    }

    @Test
    fun `substr negative start clamped to zero`() {
        // {"substr": ["abc", -10]} — negative beyond length clamps to
        // start of string, returning the full string.
        val out = StringArrayOperators.opSubstr(
            arr(s("abc"), Value.IntValue(-10)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("abc"))
    }

    @Test
    fun `substr length exceeding remaining clamps`() {
        val out = StringArrayOperators.opSubstr(
            arr(s("abc"), Value.IntValue(0), Value.IntValue(100)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("abc"))
    }

    @Test
    fun `substr slices by codepoint not char`() {
        // Multibyte UTF-8 — "café" is 4 code points but 5 bytes.
        // Slicing from index 1 should give "afé".
        val out = StringArrayOperators.opSubstr(
            arr(s("café"), Value.IntValue(1)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("afé"))
    }

    @Test
    fun `substr stringifies non-string source`() {
        // Source is coerced via `stringify` — `IntValue(12345)` becomes
        // "12345".
        val out = StringArrayOperators.opSubstr(
            arr(Value.IntValue(12345), Value.IntValue(1), Value.IntValue(3)),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(s("234"))
    }

    @Test
    fun `substr arity mismatch is type error`() {
        assertThatThrownBy {
            StringArrayOperators.opSubstr(arr(s("hello")), Value.Null, PrintlnLogger)
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
        assertThatThrownBy {
            StringArrayOperators.opSubstr(
                arr(s("hello"), Value.IntValue(0), Value.IntValue(0), Value.IntValue(0)),
                Value.Null,
                PrintlnLogger,
            )
        }.isInstanceOf(RuleError.TypeMismatch::class.java)
    }

    // ---- merge ----

    @Test
    fun `merge flattens arrays one level`() {
        val out = StringArrayOperators.opMerge(
            arr(
                arr(Value.IntValue(1), Value.IntValue(2)),
                arr(Value.IntValue(3), Value.IntValue(4)),
            ),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(
            arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3), Value.IntValue(4)),
        )
    }

    @Test
    fun `merge promotes scalars to singletons`() {
        // {"merge": [1, 2, [3, 4]]} → [1, 2, 3, 4]
        val out = StringArrayOperators.opMerge(
            arr(
                Value.IntValue(1),
                Value.IntValue(2),
                arr(Value.IntValue(3), Value.IntValue(4)),
            ),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(
            arr(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3), Value.IntValue(4)),
        )
    }

    @Test
    fun `merge zero args returns empty array`() {
        val out = StringArrayOperators.opMerge(arr(), Value.Null, PrintlnLogger)
        assertThat(out).isEqualTo(arr())
    }

    @Test
    fun `merge does not recurse on nested arrays`() {
        // Only one level of flattening — inner arrays remain.
        val out = StringArrayOperators.opMerge(
            arr(arr(arr(Value.IntValue(1)), Value.IntValue(2))),
            Value.Null,
            PrintlnLogger,
        )
        assertThat(out).isEqualTo(arr(arr(Value.IntValue(1)), Value.IntValue(2)))
    }

    // ---- helpers ----

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())

    private fun s(literal: String): Value = Value.StringValue(literal)
}
