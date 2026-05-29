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
        "var" -> AccessorOperators.opVar(args, vars)
        "missing" -> AccessorOperators.opMissing(args, vars)

        "==" -> EqualityOperators.opLooseEq(args, vars)
        "!=" -> EqualityOperators.opLooseNe(args, vars)
        "===" -> EqualityOperators.opStrictEq(args, vars)
        "!==" -> EqualityOperators.opStrictNe(args, vars)

        "!" -> LogicOperators.opNot(args, vars)
        "!!" -> LogicOperators.opNotNot(args, vars)
        "and" -> LogicOperators.opAnd(args, vars)
        "or" -> LogicOperators.opOr(args, vars)
        "if" -> LogicOperators.opIf(args, vars)

        "+" -> ArithmeticOperators.opAdd(args, vars)
        "-" -> ArithmeticOperators.opSub(args, vars)
        "*" -> ArithmeticOperators.opMul(args, vars)
        "/" -> ArithmeticOperators.opDiv(args, vars)
        "%" -> ArithmeticOperators.opMod(args, vars)

        "<" -> ComparisonOperators.opLt(args, vars)
        "<=" -> ComparisonOperators.opLe(args, vars)
        ">" -> ComparisonOperators.opGt(args, vars)
        ">=" -> ComparisonOperators.opGe(args, vars)

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
}
