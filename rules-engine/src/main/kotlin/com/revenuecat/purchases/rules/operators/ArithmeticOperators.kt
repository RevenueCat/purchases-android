package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.RulesEngineLogger
import com.revenuecat.purchases.rules.Value

/**
 * Arithmetic operators: `+`, `-`, `*`, `/`, `%`.
 *
 * All operators return [Value.FloatValue] regardless of operand types.
 * JSON Logic in JS coerces every operand through `parseFloat` before
 * arithmetic, so preserving an integer result would be a per-call
 * decision with no spec support. `looseEq` and `strictEq` already bridge
 * `IntValue(n)` ↔ `FloatValue(n.0)`, so callers comparing an arithmetic
 * result to an integer literal still get the expected answer.
 *
 * Operands that can't be coerced to a number ([Value.ObjectValue],
 * [Value.ArrayValue], unparseable strings) become [Double.NaN] and
 * propagate naturally — the final result is `FloatValue(NaN)`, which
 * `isTruthy` reports as falsy.
 *
 * Division and modulo by zero return [Value.Null] instead of `Infinity` /
 * `NaN`. JSON Logic JS produces the IEEE values, but for rule authoring
 * `Null` is friendlier: it short-circuits comparisons in a predictable
 * way and matches the engine's "missing value" convention.
 */
internal object ArithmeticOperators {

    /**
     * `{"+": [a, b, ...]}` — variadic sum. The 1-arg form acts as a
     * numeric cast (`{"+": ["3.14"]}` → `3.14`). 0 arguments is a
     * [RuleError.TypeMismatch].
     */
    fun opAdd(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        if (evaluated.isEmpty()) {
            throw RuleError.TypeMismatch("operator '+' requires at least 1 argument")
        }
        val sum = evaluated.fold(0.0) { acc, value -> acc + value.asDouble() }
        return Value.FloatValue(sum)
    }

    /**
     * `{"*": [a, b, ...]}` — variadic product. 0 arguments is a
     * [RuleError.TypeMismatch].
     */
    fun opMul(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        if (evaluated.isEmpty()) {
            throw RuleError.TypeMismatch("operator '*' requires at least 1 argument")
        }
        val product = evaluated.fold(1.0) { acc, value -> acc * value.asDouble() }
        return Value.FloatValue(product)
    }

    /**
     * `{"-": [a]}` — unary negation. `{"-": [a, b]}` — subtraction.
     * Other arities are a [RuleError.TypeMismatch].
     */
    fun opSub(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val evaluated = Operators.evalArgs(args, vars, logger)
        return when (evaluated.size) {
            1 -> Value.FloatValue(-evaluated[0].asDouble())
            2 -> Value.FloatValue(evaluated[0].asDouble() - evaluated[1].asDouble())
            else -> throw RuleError.TypeMismatch(
                "operator '-' expects 1 or 2 arguments, got ${evaluated.size}",
            )
        }
    }

    /**
     * `{"/": [a, b]}` — division. Division by zero returns [Value.Null]
     * (see type docs).
     */
    fun opDiv(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val (lhs, rhs) = Operators.evalTwo(args, vars, logger, "/")
        val divisor = rhs.asDouble()
        if (divisor == 0.0) return Value.Null
        return Value.FloatValue(lhs.asDouble() / divisor)
    }

    /**
     * `{"%": [a, b]}` — modulo. Modulo by zero returns [Value.Null] (see
     * type docs).
     */
    fun opMod(args: Value, vars: Value, logger: RulesEngineLogger): Value {
        val (lhs, rhs) = Operators.evalTwo(args, vars, logger, "%")
        val divisor = rhs.asDouble()
        if (divisor == 0.0) return Value.Null
        return Value.FloatValue(lhs.asDouble() % divisor)
    }

    /**
     * Coerce to [Double], falling back to [Double.NaN] for non-numeric
     * operands so arithmetic propagates the failure without raising an
     * error.
     */
    private fun Value.asDouble(): Double = toNumberOrNull() ?: Double.NaN
}
