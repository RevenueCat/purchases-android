package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.AccessorOperators

/**
 * `rc.rootVar` — like `var`, but reads from the root data scope.
 */
internal object RootVarOperator {

    fun opRootVar(args: Value, vars: Scope): Value =
        AccessorOperators.resolveVar(
            args = args,
            target = vars.root,
            vars = vars,
            operatorName = "rc.rootVar",
        )
}
