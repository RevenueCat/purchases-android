package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.RuleError
import com.revenuecat.purchases.rules.RulesEngineLogger
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
        logger: RulesEngineLogger,
    ): Value = when (op) {
        "var" -> AccessorOperators.opVar(args, vars, logger)
        "missing" -> AccessorOperators.opMissing(args, vars, logger)

        "==" -> EqualityOperators.opLooseEq(args, vars, logger)
        "!=" -> EqualityOperators.opLooseNe(args, vars, logger)
        "===" -> EqualityOperators.opStrictEq(args, vars, logger)
        "!==" -> EqualityOperators.opStrictNe(args, vars, logger)

        "!" -> LogicOperators.opNot(args, vars, logger)
        "!!" -> LogicOperators.opNotNot(args, vars, logger)
        "and" -> LogicOperators.opAnd(args, vars, logger)
        "or" -> LogicOperators.opOr(args, vars, logger)
        "if" -> LogicOperators.opIf(args, vars, logger)

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
        logger: RulesEngineLogger,
    ): List<Value> = argsAsList(args).map { Evaluator.evaluateValue(it, vars, logger) }

    /**
     * Evaluate exactly two arguments. Used by binary operators (`==`, `!=`,
     * `===`, `!==`, and the comparison operators a future iteration will
     * add).
     */
    fun evalTwo(
        args: Value,
        vars: Value,
        logger: RulesEngineLogger,
        opName: String,
    ): Pair<Value, Value> {
        val evaluated = evalArgs(args, vars, logger)
        if (evaluated.size != 2) {
            throw RuleError.TypeMismatch(
                "operator '$opName' expects 2 arguments, got ${evaluated.size}",
            )
        }
        return evaluated[0] to evaluated[1]
    }
}
