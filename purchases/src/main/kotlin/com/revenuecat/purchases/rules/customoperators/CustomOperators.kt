package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value

/**
 * RevenueCat-specific JSON Logic operators.
 *
 * Operators here are namespaced under `rc.` so they cannot collide with a
 * future standard JSON Logic operator.
 */
internal object CustomOperators {

    fun dispatch(
        op: String,
        args: Value,
        vars: Scope,
    ): Value = when (op) {
        "rc.length" -> LengthOperator.opLength(args, vars)

        "rc.rootVar" -> RootVarOperator.opRootVar(args, vars)

        else -> throw EvaluationException.UnsupportedOperator(op)
    }
}
