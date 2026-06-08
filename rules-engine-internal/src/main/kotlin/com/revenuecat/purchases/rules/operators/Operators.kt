package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.Value

/**
 * JSON Logic operator dispatcher and shared helpers.
 *
 * Operators are responsible for evaluating their own arguments. Most use
 * the [evalTwo] / [evalArgs] helpers which evaluate eagerly; short-circuit
 * operators (`and`, `or`, `if`) iterate manually.
 */
internal object Operators {

    /**
     * Dispatch a JSON Logic operator. Throws
     * [RuleError.UnsupportedOperator] when the operator name isn't
     * implemented in this slice.
     */
    @Suppress("ComplexMethod", "ReturnCount")
    fun dispatch(
        op: String,
        args: Value,
        vars: Value,
    ): Value = when (op) {
        // Accessors
        "var" -> AccessorOperators.opVar(args, vars)
        "missing" -> AccessorOperators.opMissing(args, vars)
        "missing_some" -> AccessorOperators.opMissingSome(args, vars)

        // Equality
        "==" -> EqualityOperators.opLooseEq(args, vars)
        "!=" -> EqualityOperators.opLooseNe(args, vars)
        "===" -> EqualityOperators.opStrictEq(args, vars)
        "!==" -> EqualityOperators.opStrictNe(args, vars)

        // Logic
        "!" -> LogicOperators.opNot(args, vars)
        "!!" -> LogicOperators.opNotNot(args, vars)
        "and" -> LogicOperators.opAnd(args, vars)
        "or" -> LogicOperators.opOr(args, vars)
        "if" -> LogicOperators.opIf(args, vars)

        // Arithmetic
        "+" -> ArithmeticOperators.opAdd(args, vars)
        "-" -> ArithmeticOperators.opSub(args, vars)
        "*" -> ArithmeticOperators.opMul(args, vars)
        "/" -> ArithmeticOperators.opDiv(args, vars)
        "%" -> ArithmeticOperators.opMod(args, vars)

        // Min and max
        "min" -> MinMaxOperators.opMin(args, vars)
        "max" -> MinMaxOperators.opMax(args, vars)

        // Comparison
        "<" -> ComparisonOperators.opLt(args, vars)
        "<=" -> ComparisonOperators.opLe(args, vars)
        ">" -> ComparisonOperators.opGt(args, vars)
        ">=" -> ComparisonOperators.opGe(args, vars)

        // String and array
        "in" -> StringArrayOperators.opIn(args, vars)
        "cat" -> StringArrayOperators.opCat(args, vars)
        "substr" -> StringArrayOperators.opSubstr(args, vars)
        "merge" -> StringArrayOperators.opMerge(args, vars)

        // Iteration
        "some" -> IterationOperators.opSome(args, vars)
        "all" -> IterationOperators.opAll(args, vars)

        else -> throw RuleError.UnsupportedOperator(op)
    }

    /**
     * Treat an operator argument as an argument list. Per JSON Logic, a
     * single-value argument is implicitly wrapped in a one-element list,
     * so `{"!": true}` and `{"!": [true]}` are equivalent.
     */
    fun argsAsList(args: Value): List<Value> = when (args) {
        is Value.ArrayValue -> args.items
        else -> listOf(args)
    }

    /** Evaluate every element in an argument list. */
    fun evalArgs(
        args: Value,
        vars: Value,
    ): List<Value> = argsAsList(args).map { Evaluator.evaluateValue(it, vars) }

    /**
     * Evaluate args and return the first two operands. Missing operands
     * default to [Value.Null] (standing in for JS `undefined`) and extras
     * are silently discarded — matches `json-logic-js`'s `function(a, b)`
     * operator signatures.
     */
    fun evalTwo(
        args: Value,
        vars: Value,
    ): Pair<Value, Value> {
        val evaluated = evalArgs(args, vars)
        val lhs = evaluated.firstOrNull() ?: Value.Null
        val rhs = if (evaluated.size >= 2) evaluated[1] else Value.Null
        return lhs to rhs
    }

    /**
     * Safely truncate a [Double] to [Int] for index / count math.
     * `NaN` → `0` (matches JS `ToInteger`); `±Infinity` and
     * out-of-range finite values clamp to [Int.MAX_VALUE] / [Int.MIN_VALUE].
     */
    @Suppress("ReturnCount")
    fun clampedInt(value: Double): Int {
        if (value.isNaN()) return 0
        if (value >= Int.MAX_VALUE.toDouble()) return Int.MAX_VALUE
        if (value <= Int.MIN_VALUE.toDouble()) return Int.MIN_VALUE
        return value.toInt()
    }
}
