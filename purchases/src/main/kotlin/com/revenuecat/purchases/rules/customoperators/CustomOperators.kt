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

    @Suppress("CyclomaticComplexMethod")
    fun dispatch(
        op: String,
        args: Value,
        vars: Scope,
    ): Value = when (op) {
        "rc.entries" -> EntriesOperators.opEntries(args, vars)
        "rc.fromEntries" -> EntriesOperators.opFromEntries(args, vars)

        "rc.let" -> LetOperator.opLet(args, vars)

        "rc.lower" -> CaseOperators.opLower(args, vars)
        "rc.upper" -> CaseOperators.opUpper(args, vars)

        "rc.regexMatch" -> RegexOperators.opRegexMatch(args, vars)

        "rc.rootVar" -> RootVarOperator.opRootVar(args, vars)

        "rc.semverCompare" -> SemverOperator.opSemverCompare(args, vars)

        "rc.slice" -> SliceOperator.opSlice(args, vars)

        "rc.sortBy" -> SortByOperator.opSortBy(args, vars)

        "rc.split" -> SplitOperator.opSplit(args, vars)

        else -> throw EvaluationException.UnsupportedOperator(op)
    }
}
