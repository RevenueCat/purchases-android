package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.CapturingLoggerRule
import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class LogicOperatorsTest {

    @get:Rule
    internal val loggerRule = CapturingLoggerRule()

    // ---- ! ----

    @Test
    fun `not negates truthy to false`() {
        assertThat(LogicOperators.opNot(Value.BoolValue(true), Value.Null))
            .isEqualTo(Value.BoolValue(false))
        assertThat(LogicOperators.opNot(Value.IntValue(5), Value.Null))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `not negates falsy to true`() {
        assertThat(LogicOperators.opNot(Value.BoolValue(false), Value.Null))
            .isEqualTo(Value.BoolValue(true))
        assertThat(LogicOperators.opNot(Value.IntValue(0), Value.Null))
            .isEqualTo(Value.BoolValue(true))
        assertThat(LogicOperators.opNot(Value.Null, Value.Null))
            .isEqualTo(Value.BoolValue(true))
    }

    @Test
    fun `not unwraps singleton array`() {
        assertThat(
            LogicOperators.opNot(
                Value.ArrayValue(listOf(Value.BoolValue(true))),
                Value.Null,
            ),
        ).isEqualTo(Value.BoolValue(false))
    }

    // ---- !! ----

    @Test
    fun `notNot casts to bool`() {
        assertThat(LogicOperators.opNotNot(Value.IntValue(5), Value.Null))
            .isEqualTo(Value.BoolValue(true))
        assertThat(LogicOperators.opNotNot(Value.StringValue(""), Value.Null))
            .isEqualTo(Value.BoolValue(false))
    }

    // ---- and ----

    @Test
    fun `and returns first falsy value`() {
        val args = Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(0), Value.IntValue(2)))
        assertThat(LogicOperators.opAnd(args, Value.Null))
            .isEqualTo(Value.IntValue(0)) // first falsy
    }

    @Test
    fun `and returns last value when all truthy`() {
        val args = Value.ArrayValue(listOf(Value.IntValue(1), Value.IntValue(2), Value.IntValue(3)))
        assertThat(LogicOperators.opAnd(args, Value.Null))
            .isEqualTo(Value.IntValue(3))
    }

    @Test
    fun `and short-circuits on first falsy`() {
        // Second arg is falsy; third would error if evaluated (unsupported op).
        val args = Value.ArrayValue(
            listOf(
                Value.IntValue(1),
                Value.BoolValue(false),
                Value.ObjectValue(
                    mapOf(
                        "definitelyNotAnOp" to Value.ArrayValue(listOf(Value.IntValue(1))),
                    ),
                ),
            ),
        )
        assertThat(LogicOperators.opAnd(args, Value.Null))
            .isEqualTo(Value.BoolValue(false))
    }

    @Test
    fun `and empty is true`() {
        assertThat(
            LogicOperators.opAnd(Value.ArrayValue(emptyList()), Value.Null),
        ).isEqualTo(Value.BoolValue(true))
    }

    // ---- or ----

    @Test
    fun `or returns first truthy value`() {
        val args = Value.ArrayValue(listOf(Value.IntValue(0), Value.IntValue(7), Value.IntValue(2)))
        assertThat(LogicOperators.opOr(args, Value.Null))
            .isEqualTo(Value.IntValue(7))
    }

    @Test
    fun `or returns last value when all falsy`() {
        val args = Value.ArrayValue(listOf(Value.IntValue(0), Value.BoolValue(false), Value.Null))
        assertThat(LogicOperators.opOr(args, Value.Null))
            .isEqualTo(Value.Null)
    }

    @Test
    fun `or empty is false`() {
        assertThat(
            LogicOperators.opOr(Value.ArrayValue(emptyList()), Value.Null),
        ).isEqualTo(Value.BoolValue(false))
    }

    // ---- if ----

    @Test
    fun `if three-arg form`() {
        val yesNoTrue = Value.ArrayValue(
            listOf(Value.BoolValue(true), Value.StringValue("yes"), Value.StringValue("no")),
        )
        assertThat(LogicOperators.opIf(yesNoTrue, Value.Null))
            .isEqualTo(Value.StringValue("yes"))

        val yesNoFalse = Value.ArrayValue(
            listOf(Value.BoolValue(false), Value.StringValue("yes"), Value.StringValue("no")),
        )
        assertThat(LogicOperators.opIf(yesNoFalse, Value.Null))
            .isEqualTo(Value.StringValue("no"))
    }

    @Test
    fun `if chained else-if`() {
        // if (false) "a" else if (true) "b" else "c"  →  "b"
        val args = Value.ArrayValue(
            listOf(
                Value.BoolValue(false),
                Value.StringValue("a"),
                Value.BoolValue(true),
                Value.StringValue("b"),
                Value.StringValue("c"),
            ),
        )
        assertThat(LogicOperators.opIf(args, Value.Null))
            .isEqualTo(Value.StringValue("b"))
    }

    @Test
    fun `if no truthy no else returns null`() {
        // Even-arity, no else, no truthy condition.
        val args = Value.ArrayValue(
            listOf(
                Value.BoolValue(false),
                Value.StringValue("a"),
                Value.BoolValue(false),
                Value.StringValue("b"),
            ),
        )
        assertThat(LogicOperators.opIf(args, Value.Null))
            .isEqualTo(Value.Null)
    }

    @Test
    fun `if empty returns null`() {
        assertThat(
            LogicOperators.opIf(Value.ArrayValue(emptyList()), Value.Null),
        ).isEqualTo(Value.Null)
    }

    @Test
    fun `if single-arg form returns the evaluated arg`() {
        // `{"if": [expr]}` falls through to the trailing-else branch and
        // returns the evaluated arg unchanged. Pinning so a future refactor
        // doesn't silently flip this to "always Null" or "always Bool-coerced".
        val args = Value.ArrayValue(listOf(Value.StringValue("only")))
        assertThat(LogicOperators.opIf(args, Value.Null))
            .isEqualTo(Value.StringValue("only"))
    }

    @Test
    fun `if two-arg form returns then when truthy and null otherwise`() {
        // `{"if": [cond, then]}` — no else clause. Truthy → returns evaluated
        // `then`; falsy → returns Null.
        val truthy = Value.ArrayValue(
            listOf(Value.BoolValue(true), Value.StringValue("yes")),
        )
        assertThat(LogicOperators.opIf(truthy, Value.Null))
            .isEqualTo(Value.StringValue("yes"))

        val falsy = Value.ArrayValue(
            listOf(Value.BoolValue(false), Value.StringValue("yes")),
        )
        assertThat(LogicOperators.opIf(falsy, Value.Null))
            .isEqualTo(Value.Null)
    }
}
